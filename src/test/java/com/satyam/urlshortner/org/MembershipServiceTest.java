package com.satyam.urlshortner.org;

import com.satyam.urlshortner.auth.AuthPrincipal;
import com.satyam.urlshortner.auth.User;
import com.satyam.urlshortner.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private UserRepository userRepository;

    private MembershipService membershipService;

    private static final AuthPrincipal MEMBER = AuthPrincipal.forUser(1L, "member@acme.test", 10L, Role.MEMBER);
    private static final AuthPrincipal ADMIN = AuthPrincipal.forUser(2L, "admin@acme.test", 10L, Role.ADMIN);

    @BeforeEach
    void setUp() {
        membershipService = new MembershipService(organizationRepository, membershipRepository, invitationRepository, userRepository);
    }

    @Test
    void inviteMemberRejectedForMemberRole() {
        StepVerifier.create(membershipService.inviteMember(MEMBER, new InviteMemberRequest("new@acme.test", Role.MEMBER)))
                .expectError(AccessDeniedForRoleException.class)
                .verify();

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void inviteMemberSucceedsForAdminRole() {
        when(invitationRepository.save(any())).thenAnswer(inv -> Mono.just(withId(inv.getArgument(0), 99L)));

        StepVerifier.create(membershipService.inviteMember(ADMIN, new InviteMemberRequest("new@acme.test", Role.MEMBER)))
                .assertNext(response -> {
                    assertEquals("new@acme.test", response.email());
                    assertEquals(Role.MEMBER, response.role());
                })
                .verifyComplete();
    }

    @Test
    void acceptInvitationFailsWhenTokenUnknown() {
        when(invitationRepository.findByToken("missing")).thenReturn(Mono.empty());

        StepVerifier.create(membershipService.acceptInvitation(MEMBER, "missing"))
                .expectError(InvitationNotFoundException.class)
                .verify();
    }

    @Test
    void acceptInvitationFailsWhenExpired() {
        Invitation expired = new Invitation(1L, 10L, MEMBER.email(), Role.MEMBER, "tok", 2L,
                Instant.now().minusSeconds(10), null, Instant.now());
        when(invitationRepository.findByToken("tok")).thenReturn(Mono.just(expired));

        StepVerifier.create(membershipService.acceptInvitation(MEMBER, "tok"))
                .expectError(InvitationExpiredException.class)
                .verify();
    }

    @Test
    void acceptInvitationFailsWhenEmailMismatch() {
        Invitation invitation = new Invitation(1L, 10L, "someone-else@acme.test", Role.MEMBER, "tok", 2L,
                Instant.now().plusSeconds(3600), null, Instant.now());
        when(invitationRepository.findByToken("tok")).thenReturn(Mono.just(invitation));

        StepVerifier.create(membershipService.acceptInvitation(MEMBER, "tok"))
                .expectError(InvitationEmailMismatchException.class)
                .verify();
    }

    @Test
    void acceptInvitationCreatesMembershipOnSuccess() {
        Invitation invitation = new Invitation(1L, 10L, MEMBER.email(), Role.MEMBER, "tok", 2L,
                Instant.now().plusSeconds(3600), null, Instant.now());
        User user = new User(MEMBER.userId(), MEMBER.email(), "hashed", "Member", Instant.now());

        when(invitationRepository.findByToken("tok")).thenReturn(Mono.just(invitation));
        when(membershipRepository.save(any())).thenAnswer(inv -> Mono.just(withId(inv.getArgument(0), 55L)));
        when(invitationRepository.acceptByToken(eq("tok"), any())).thenReturn(Mono.just(1));
        when(userRepository.findById(MEMBER.userId())).thenReturn(Mono.just(user));

        StepVerifier.create(membershipService.acceptInvitation(MEMBER, "tok"))
                .assertNext(response -> assertEquals(Role.MEMBER, response.role()))
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    private <T> T withId(Object entity, Long id) {
        if (entity instanceof Invitation invitation) {
            return (T) new Invitation(id, invitation.getOrgId(), invitation.getEmail(), invitation.getRole(),
                    invitation.getToken(), invitation.getInvitedBy(), invitation.getExpiresAt(), invitation.getAcceptedAt(), invitation.getCreatedAt());
        }
        if (entity instanceof Membership membership) {
            return (T) new Membership(id, membership.getUserId(), membership.getOrgId(), membership.getRole(), membership.getCreatedAt());
        }
        throw new IllegalArgumentException("Unsupported entity: " + entity);
    }
}
