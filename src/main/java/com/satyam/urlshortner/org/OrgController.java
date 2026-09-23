package com.satyam.urlshortner.org;

import com.satyam.urlshortner.auth.AuthPrincipal;
import com.satyam.urlshortner.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final MembershipService membershipService;

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
    public Mono<MemberResponse> acceptInvitation(@PathVariable String token) {
        return CurrentUser.get().flatMap(principal -> membershipService.acceptInvitation(principal, token));
    }
}
