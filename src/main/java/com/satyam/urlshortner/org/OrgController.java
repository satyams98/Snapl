package com.satyam.urlshortner.org;

import com.satyam.urlshortner.auth.AuthResponse;
import com.satyam.urlshortner.auth.AuthService;
import com.satyam.urlshortner.auth.CurrentUser;
import com.satyam.urlshortner.auth.RefreshCookieIssuer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final MembershipService membershipService;
    private final AuthService authService;
    private final RefreshCookieIssuer refreshCookieIssuer;

    @GetMapping("/me")
    public Mono<OrganizationResponse> currentOrganization() {
        return CurrentUser.get().flatMap(membershipService::getCurrentOrganization);
    }

    @GetMapping("/me/members")
    public Flux<MemberResponse> members() {
        return CurrentUser.get().flatMapMany(membershipService::listMembers);
    }

    @PostMapping("/me/invitations")
    public Mono<InvitationResponse> invite(@Valid @RequestBody InviteMemberRequest request) {
        return CurrentUser.get().flatMap(principal -> membershipService.inviteMember(principal, request));
    }

    @PostMapping("/invitations/{token}/accept")
    public Mono<ResponseEntity<AuthResponse>> acceptInvitation(@PathVariable String token, ServerHttpResponse response) {
        return CurrentUser.get()
                .flatMap(principal -> membershipService.acceptInvitation(principal, token))
                .flatMap(accepted -> authService.issueTokensForMembership(accepted.user(), accepted.membership(), accepted.organization()))
                .map(result -> refreshCookieIssuer.issue(result, response));
    }
}
