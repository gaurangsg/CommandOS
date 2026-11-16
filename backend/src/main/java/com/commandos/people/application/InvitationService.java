package com.commandos.people.application;

import com.commandos.identity.application.AuthService;
import com.commandos.identity.domain.UserEntity;
import com.commandos.identity.persistence.UserRepository;
import com.commandos.organization.domain.DepartmentEntity;
import com.commandos.organization.domain.MembershipEntity;
import com.commandos.organization.domain.OrganizationEntity;
import com.commandos.organization.domain.RoleEntity;
import com.commandos.organization.persistence.DepartmentRepository;
import com.commandos.organization.persistence.MembershipRepository;
import com.commandos.organization.persistence.OrganizationRepository;
import com.commandos.organization.persistence.RoleRepository;
import com.commandos.people.api.dto.InviteEmployeeRequest;
import com.commandos.people.api.dto.InvitationPreviewResponse;
import com.commandos.people.api.dto.InvitationResponse;
import com.commandos.people.domain.InvitationEntity;
import com.commandos.people.persistence.InvitationRepository;
import com.commandos.shared.api.ApiException;
import com.commandos.shared.security.TokenHash;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private static final Duration INVITATION_LIFETIME = Duration.ofDays(7);
    private static final String MEMBER_ROLE = "Member";

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String mailHost;
    private final String mailFrom;
    private final String frontendBaseUrl;

    public InvitationService(
            InvitationRepository invitationRepository,
            OrganizationRepository organizationRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            Clock clock,
            @Value("${commandos.mail.host:}") String mailHost,
            @Value("${commandos.mail.from:}") String mailFrom,
            @Value("${commandos.mail.frontend-base-url}") String frontendBaseUrl) {
        this.invitationRepository = invitationRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.mailSenderProvider = mailSenderProvider;
        this.clock = clock;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public InvitationResponse invite(UUID organizationId, UUID inviterUserId, InviteEmployeeRequest request) {
        JavaMailSender mailSender = configuredMailSender();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ACCOUNT_ALREADY_EXISTS",
                    "This email already has a CommandOS account; inviting existing accounts is not supported yet.");
        }

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "The workspace was not found."));
        RoleEntity memberRole = roleRepository.findByOrganizationIdAndName(organizationId, MEMBER_ROLE)
                .orElseThrow(() -> new IllegalStateException("The default Member role is missing."));
        DepartmentEntity department = request.departmentId() == null
                ? null
                : departmentRepository.findByIdAndOrganizationId(request.departmentId(), organizationId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_DEPARTMENT",
                                "The department must belong to this workspace."));

        Instant now = clock.instant();
        invitationRepository.revokePending(organizationId, email, now);
        byte[] rawToken = new byte[32];
        secureRandom.nextBytes(rawToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken);
        UserEntity inviter = userRepository.getReferenceById(inviterUserId);
        InvitationEntity invitation = invitationRepository.saveAndFlush(new InvitationEntity(
                UUID.randomUUID(),
                organization,
                email,
                request.displayName().trim(),
                memberRole,
                department,
                inviter,
                TokenHash.sha256(token),
                now.plus(INVITATION_LIFETIME)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("You're invited to " + organization.getName() + " on CommandOS");
        message.setText("""
                Hello %s,

                %s invited you to join %s on CommandOS.
                Accept your invitation within 7 days:
                %s/accept-invitation?token=%s

                If you were not expecting this invitation, you can ignore this message.
                """.formatted(
                request.displayName().trim(),
                inviter.getDisplayName(),
                organization.getName(),
                frontendBaseUrl,
                token));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "INVITATION_DELIVERY_FAILED",
                    "The invitation email could not be delivered. Try again later.");
        }
        return toResponse(invitation, now);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> list(UUID organizationId) {
        Instant now = clock.instant();
        return invitationRepository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(invitation -> toResponse(invitation, now))
                .toList();
    }

    @Transactional
    public void revoke(UUID organizationId, UUID invitationId) {
        InvitationEntity invitation = invitationRepository.findByIdAndOrganizationId(invitationId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND", "The invitation was not found."));
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "INVITATION_NOT_PENDING", "Only pending invitations can be revoked.");
        }
        invitation.revoke(clock.instant());
    }

    @Transactional(readOnly = true)
    public InvitationPreviewResponse preview(String token) {
        InvitationEntity invitation = invitationRepository.findByTokenHash(TokenHash.sha256(token))
                .orElseThrow(() -> invalidInvitation());
        ensurePending(invitation, clock.instant());
        return new InvitationPreviewResponse(
                invitation.getInvitedEmail(),
                invitation.getDisplayName(),
                invitation.getOrganization().getName(),
                invitation.getOrganization().getSlug(),
                invitation.getDepartment() == null ? null : invitation.getDepartment().getName(),
                invitation.getExpiresAt());
    }

    @Transactional
    public AuthService.IssuedSession accept(String token, String displayName, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD",
                    "Password must not exceed 72 UTF-8 bytes.");
        }
        InvitationEntity invitation = invitationRepository.findByTokenHashForUpdate(TokenHash.sha256(token))
                .orElseThrow(() -> invalidInvitation());
        ensurePending(invitation, clock.instant());
        if (userRepository.existsByEmail(invitation.getInvitedEmail())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ACCOUNT_ALREADY_EXISTS",
                    "This invitation cannot be used because the email already has an account.");
        }

        Instant now = clock.instant();
        UserEntity user = userRepository.saveAndFlush(new UserEntity(
                UUID.randomUUID(),
                invitation.getInvitedEmail(),
                passwordEncoder.encode(password),
                displayName.trim()));
        MembershipEntity membership = membershipRepository.save(new MembershipEntity(
                UUID.randomUUID(),
                invitation.getOrganization(),
                user,
                invitation.getRole(),
                now));
        membership.updateEmployeeProfile(
                invitation.getDepartment(), null, null, "FULL_TIME", null);
        invitation.accept(now);
        return authService.createSession(membership);
    }

    private JavaMailSender configuredMailSender() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailHost.isBlank() || mailFrom.isBlank() || mailSender == null) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_DELIVERY_NOT_CONFIGURED",
                    "Employee invitations are unavailable until SMTP host and sender settings are configured.");
        }
        return mailSender;
    }

    private static void ensurePending(InvitationEntity invitation, Instant now) {
        if (!"PENDING".equals(invitation.getStatus())) {
            throw invalidInvitation();
        }
        if (!invitation.getExpiresAt().isAfter(now)) {
            throw new ApiException(
                    HttpStatus.GONE, "INVITATION_EXPIRED", "This invitation has expired.");
        }
    }

    private static InvitationResponse toResponse(InvitationEntity invitation, Instant now) {
        String status = "PENDING".equals(invitation.getStatus()) && !invitation.getExpiresAt().isAfter(now)
                ? "EXPIRED"
                : invitation.getStatus();
        return new InvitationResponse(
                invitation.getId(),
                invitation.getInvitedEmail(),
                invitation.getDisplayName(),
                status,
                invitation.getDepartment() == null ? null : invitation.getDepartment().getId(),
                invitation.getExpiresAt());
    }

    private static ApiException invalidInvitation() {
        return new ApiException(
                HttpStatus.GONE, "INVALID_INVITATION", "This invitation is invalid, expired, or no longer active.");
    }
}
