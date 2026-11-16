package com.commandos.people.application;

import com.commandos.identity.application.AuthService;
import com.commandos.identity.domain.UserEntity;
import com.commandos.identity.persistence.UserRepository;
import com.commandos.organization.domain.OrganizationEntity;
import com.commandos.organization.domain.RoleEntity;
import com.commandos.organization.persistence.DepartmentRepository;
import com.commandos.organization.persistence.MembershipRepository;
import com.commandos.organization.persistence.OrganizationRepository;
import com.commandos.organization.persistence.RoleRepository;
import com.commandos.identity.domain.RefreshTokenEntity;
import com.commandos.identity.api.dto.AuthSessionResponse;
import com.commandos.identity.api.dto.AuthUserResponse;
import com.commandos.people.api.dto.InviteEmployeeRequest;
import com.commandos.people.domain.InvitationEntity;
import com.commandos.people.persistence.InvitationRepository;
import com.commandos.shared.api.ApiException;
import com.commandos.shared.security.TokenHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private InvitationRepository invitationRepository;
    private OrganizationRepository organizationRepository;
    private RoleRepository roleRepository;
    private DepartmentRepository departmentRepository;
    private UserRepository userRepository;
    private MembershipRepository membershipRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        invitationRepository = mock(InvitationRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        roleRepository = mock(RoleRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        userRepository = mock(UserRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = mock(AuthService.class);
        mailSenderProvider = mock(ObjectProvider.class);
    }

    @Test
    void inviteFailsExplicitlyWhenEmailDeliveryIsNotConfigured() {
        InvitationService service = service("", "", mailSenderProvider);

        assertThatThrownBy(() -> service.invite(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new InviteEmployeeRequest("jane@example.com", "Jane Doe", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        verify(organizationRepository, never()).findById(any());
        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void inviteStoresOnlyTokenHashAndUsesInviterDisplayNameInEmail() {
        UUID organizationId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        OrganizationEntity organization = mock(OrganizationEntity.class);
        RoleEntity memberRole = mock(RoleEntity.class);
        UserEntity inviter = mock(UserEntity.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organization.getName()).thenReturn("Northstar");
        when(roleRepository.findByOrganizationIdAndName(organizationId, "Member"))
                .thenReturn(Optional.of(memberRole));
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.getReferenceById(inviterId)).thenReturn(inviter);
        when(inviter.getDisplayName()).thenReturn("Alex Morgan");
        when(invitationRepository.saveAndFlush(any(InvitationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        InvitationService service = service("smtp.example.test", "noreply@example.test", mailSenderProvider);

        service.invite(
                organizationId,
                inviterId,
                new InviteEmployeeRequest("Jane@example.com", "Jane Doe", null));

        var savedInvitation = org.mockito.ArgumentCaptor.forClass(InvitationEntity.class);
        verify(invitationRepository).saveAndFlush(savedInvitation.capture());
        var sentMessage = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sentMessage.capture());
        assertThat(savedInvitation.getValue().getInvitedEmail()).isEqualTo("jane@example.com");
        assertThat(savedInvitation.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(7 * 24 * 60 * 60));
        assertThat(sentMessage.getValue().getText()).contains("Alex Morgan invited you to join Northstar");
        String rawToken = sentMessage.getValue().getText().lines()
                .filter(line -> line.contains("accept-invitation?token="))
                .map(line -> line.substring(line.indexOf("token=") + "token=".length()))
                .findFirst()
                .orElseThrow();
        assertThat(MessageDigest.isEqual(
                savedInvitation.getValue().getTokenHash(),
                TokenHash.sha256(rawToken))).isTrue();
    }

    @Test
    void previewRejectsAnInvitationThatExpiredAtTheInjectedClockTime() {
        String rawToken = "invitation-token";
        OrganizationEntity organization = mock(OrganizationEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        UserEntity inviter = mock(UserEntity.class);
        InvitationEntity invitation = new InvitationEntity(
                UUID.randomUUID(),
                organization,
                "jane@example.com",
                "Jane Doe",
                role,
                null,
                inviter,
                TokenHash.sha256(rawToken),
                NOW.minusSeconds(1));
        when(invitationRepository.findByTokenHash(any(byte[].class))).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service("", "", mailSenderProvider).preview(rawToken))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getStatus())
                        .isEqualTo(HttpStatus.GONE));
        assertThat(invitation.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void acceptanceCreatesSessionAndPreventsInvitationReplay() {
        String rawToken = "single-use-token";
        OrganizationEntity organization = mock(OrganizationEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        UserEntity inviter = mock(UserEntity.class);
        InvitationEntity invitation = new InvitationEntity(
                UUID.randomUUID(),
                organization,
                "jane@example.com",
                "Jane Doe",
                role,
                null,
                inviter,
                TokenHash.sha256(rawToken),
                NOW.plusSeconds(60));
        AuthService.IssuedSession expectedSession = new AuthService.IssuedSession(
                "refresh-token",
                new AuthSessionResponse(
                        "access-token",
                        "Bearer",
                        900,
                        new AuthUserResponse(
                                UUID.randomUUID(),
                                "jane@example.com",
                                "Jane Doe",
                                UUID.randomUUID(),
                                "Northstar",
                                "northstar",
                                "Member",
                                Set.of("people:read"))),
                mock(RefreshTokenEntity.class));
        when(invitationRepository.findByTokenHashForUpdate(any(byte[].class))).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("correct-horse-battery")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.createSession(any())).thenReturn(expectedSession);
        InvitationService service = service("", "", mailSenderProvider);

        assertThat(service.accept(rawToken, "Jane Doe", "correct-horse-battery")).isSameAs(expectedSession);
        assertThat(invitation.getStatus()).isEqualTo("ACCEPTED");
        verify(authService).createSession(any());
        assertThatThrownBy(() -> service.accept(rawToken, "Jane Doe", "correct-horse-battery"))
                .isInstanceOf(ApiException.class);
        verify(authService, org.mockito.Mockito.times(1)).createSession(any());
    }

    private InvitationService service(
            String mailHost,
            String mailFrom,
            ObjectProvider<JavaMailSender> provider) {
        return new InvitationService(
                invitationRepository,
                organizationRepository,
                roleRepository,
                departmentRepository,
                userRepository,
                membershipRepository,
                passwordEncoder,
                authService,
                provider,
                Clock.fixed(NOW, ZoneOffset.UTC),
                mailHost,
                mailFrom,
                "https://app.example.test");
    }
}
