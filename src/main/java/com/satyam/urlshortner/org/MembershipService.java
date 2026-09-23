package com.satyam.urlshortner.org;

import com.satyam.urlshortner.auth.AuthPrincipal;
import com.satyam.urlshortner.auth.User;
import com.satyam.urlshortner.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private static final Set<Role> CAN_MANAGE_MEMBERS = EnumSet.of(Role.OWNER, Role.ADMIN);
    private static final java.time.Duration INVITATION_TTL = java.time.Duration.ofDays(7);

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;

    public Mono<OrganizationResponse> getCurrentOrganization(AuthPrincipal principal) {
        return organizationRepository.findById(principal.orgId())
                .map(org -> new OrganizationResponse(org.getId(), org.getName(), org.getSlug(), principal.role()));
    }

    public Flux<MemberResponse> listMembers(AuthPrincipal principal) {
        return membershipRepository.findByOrgId(principal.orgId())
                .flatMap(membership -> userRepository.findById(membership.getUserId())
                        .map(user -> new MemberResponse(user.getId(), user.getEmail(), user.getName(), membership.getRole())));
    }

    public Mono<InvitationResponse> inviteMember(AuthPrincipal principal, InviteMemberRequest request) {
        return requireManagerRole(principal)
                .then(Mono.defer(() -> {
                    Invitation invitation = new Invitation(null, principal.orgId(), request.email(), request.role(),
                            UUID.randomUUID().toString().replace("-", ""), principal.userId(),
                            Instant.now().plus(INVITATION_TTL), null, Instant.now());
                    return invitationRepository.save(invitation);
                }))
                .map(saved -> new InvitationResponse(saved.getId(), saved.getEmail(), saved.getRole(), saved.getToken(), saved.getExpiresAt()));
    }

    public Mono<MemberResponse> acceptInvitation(AuthPrincipal principal, String token) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException(token)))
                .flatMap(invitation -> validateInvitation(invitation, principal))
                .flatMap(invitation -> membershipRepository.save(new Membership(null, principal.userId(), invitation.getOrgId(), invitation.getRole(), Instant.now()))
                        .flatMap(membership -> invitationRepository.acceptByToken(token, Instant.now())
                                .then(userRepository.findById(principal.userId()))
                                .map(user -> new MemberResponse(user.getId(), user.getEmail(), user.getName(), membership.getRole()))));
    }

    private Mono<Invitation> validateInvitation(Invitation invitation, AuthPrincipal principal) {
        if (invitation.getAcceptedAt() != null || invitation.getExpiresAt().isBefore(Instant.now())) {
            return Mono.error(new InvitationExpiredException(invitation.getToken()));
        }
        if (!invitation.getEmail().equalsIgnoreCase(principal.email())) {
            return Mono.error(new InvitationEmailMismatchException());
        }
        return Mono.just(invitation);
    }

    private Mono<Void> requireManagerRole(AuthPrincipal principal) {
        return CAN_MANAGE_MEMBERS.contains(principal.role())
                ? Mono.empty()
                : Mono.error(new AccessDeniedForRoleException(Role.ADMIN));
    }
}
