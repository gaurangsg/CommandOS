package com.commandos.identity.application;

import com.commandos.identity.api.dto.AuthSessionResponse;
import com.commandos.identity.api.dto.AuthUserResponse;
import com.commandos.identity.api.dto.LoginRequest;
import com.commandos.identity.api.dto.RefreshRequest;
import com.commandos.identity.api.dto.RegisterRequest;
import com.commandos.identity.domain.RefreshTokenEntity;
import com.commandos.identity.domain.UserEntity;
import com.commandos.identity.persistence.RefreshTokenRepository;
import com.commandos.identity.persistence.UserRepository;
import com.commandos.organization.domain.MembershipEntity;
import com.commandos.organization.domain.OrganizationEntity;
import com.commandos.organization.domain.PermissionEntity;
import com.commandos.organization.domain.RoleEntity;
import com.commandos.organization.persistence.MembershipRepository;
import com.commandos.organization.persistence.OrganizationRepository;
import com.commandos.organization.persistence.PermissionRepository;
import com.commandos.organization.persistence.RoleRepository;
import com.commandos.shared.api.ApiException;
import com.commandos.shared.security.TokenHash;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final List<String> OWNER_PERMISSIONS = List.of(
            "organization:read",
            "organization:manage",
            "members:read",
            "members:manage",
            "roles:manage",
            "audit:read",
            "people:read",
            "people:manage",
            "departments:read",
            "departments:manage");
    private static final List<String> MEMBER_PERMISSIONS = List.of(
            "organization:read",
            "people:read",
            "departments:read");
    private static final String INVALID_LOGIN = "Email, password, or workspace is incorrect.";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final MembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;
    private final Duration refreshTokenLifetime;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            MembershipRepository membershipRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            Clock clock,
            @Value("${commandos.security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
        if (refreshTokenDays < 1 || refreshTokenDays > 90) {
            throw new IllegalArgumentException("Refresh-token lifetime must be between 1 and 90 days");
        }
        this.refreshTokenLifetime = Duration.ofDays(refreshTokenDays);
    }

    @Transactional
    public IssuedSession register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        String slug = request.organizationSlug().trim().toLowerCase(java.util.Locale.ROOT);
        if (passwordBytes(request.password()).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD",
                    "Password must not exceed 72 UTF-8 bytes.");
        }
        if (userRepository.existsByEmail(email) || organizationRepository.existsBySlug(slug)) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_OR_WORKSPACE_EXISTS",
                    "That email or workspace address is already in use.");
        }

        List<PermissionEntity> permissions = permissionRepository.findAllByPermissionKeyIn(OWNER_PERMISSIONS);
        List<PermissionEntity> memberPermissions = permissionRepository.findAllByPermissionKeyIn(MEMBER_PERMISSIONS);
        if (permissions.size() != OWNER_PERMISSIONS.size()
                || memberPermissions.size() != MEMBER_PERMISSIONS.size()) {
            throw new IllegalStateException("The permission catalog is incomplete; apply the latest database migration.");
        }

        OrganizationEntity organization = organizationRepository.saveAndFlush(
                new OrganizationEntity(UUID.randomUUID(), request.organizationName().trim(), slug));
        UserEntity user = userRepository.saveAndFlush(new UserEntity(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()));
        RoleEntity ownerRole = roleRepository.save(new RoleEntity(
                UUID.randomUUID(), organization, "Owner", "Full access to this workspace", true));
        ownerRole.setPermissions(permissions);
        roleRepository.save(ownerRole);
        RoleEntity memberRole = roleRepository.save(new RoleEntity(
                UUID.randomUUID(), organization, "Member", "Standard workspace member", true));
        memberRole.setPermissions(memberPermissions);
        roleRepository.save(memberRole);
        MembershipEntity membership = membershipRepository.save(new MembershipEntity(
                UUID.randomUUID(), organization, user, ownerRole, clock.instant()));

        return issueSession(membership, UUID.randomUUID());
    }

    @Transactional
    public IssuedSession login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        String slug = request.organizationSlug().trim().toLowerCase(java.util.Locale.ROOT);
        MembershipEntity membership = membershipRepository.findForAuthentication(email, slug)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_LOGIN));
        UserEntity user = membership.getUser();
        if (!"ACTIVE".equals(user.getStatus())
                || !"ACTIVE".equals(membership.getStatus())
                || !"ACTIVE".equals(membership.getOrganization().getStatus())
                || passwordBytes(request.password()).length > 72
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_LOGIN);
        }
        return issueSession(membership, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public IssuedSession refresh(String rawToken, RefreshRequest request) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "A valid refresh session is required.");
        }
        byte[] tokenHash = TokenHash.sha256(rawToken);
        RefreshTokenEntity oldToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "The refresh session is invalid or expired."));
        Instant now = clock.instant();
        if (oldToken.getRevokedAt() != null) {
            if (oldToken.getReplacedBy() != null) {
                refreshTokenRepository.revokeActiveFamily(oldToken.getFamilyId(), now);
            }
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "The refresh session is invalid or expired.");
        }
        if (!oldToken.getExpiresAt().isAfter(now)) {
            oldToken.revoke(now);
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "The refresh session is invalid or expired.");
        }

        MembershipEntity membership = membershipRepository.findForRefresh(
                        oldToken.getUser().getId(),
                        request.organizationSlug().trim().toLowerCase(java.util.Locale.ROOT))
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus())
                        && "ACTIVE".equals(candidate.getUser().getStatus())
                        && "ACTIVE".equals(candidate.getOrganization().getStatus()))
                .orElse(null);
        if (membership == null) {
            refreshTokenRepository.revokeActiveFamily(oldToken.getFamilyId(), now);
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "The refresh session is invalid or expired.");
        }

        IssuedSession replacement = issueSession(membership, oldToken.getFamilyId());
        oldToken.revoke(now, replacement.tokenRecord());
        refreshTokenRepository.save(oldToken);
        return replacement;
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(TokenHash.sha256(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> refreshTokenRepository.revokeActiveFamily(token.getFamilyId(), clock.instant()));
    }

    @Transactional
    public IssuedSession createSession(MembershipEntity membership) {
        return issueSession(membership, UUID.randomUUID());
    }

    private IssuedSession issueSession(MembershipEntity membership, UUID familyId) {
        AuthUserResponse user = toUserResponse(membership);
        String accessToken = jwtTokenService.create(user);
        byte[] randomToken = new byte[32];
        secureRandom.nextBytes(randomToken);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        RefreshTokenEntity persistedToken = new RefreshTokenEntity(
                UUID.randomUUID(),
                familyId,
                membership.getUser(),
                TokenHash.sha256(refreshToken),
                clock.instant().plus(refreshTokenLifetime));
        refreshTokenRepository.saveAndFlush(persistedToken);
        return new IssuedSession(
                refreshToken,
                new AuthSessionResponse(accessToken, "Bearer", jwtTokenService.getAccessTokenSeconds(), user),
                persistedToken);
    }

    private AuthUserResponse toUserResponse(MembershipEntity membership) {
        Set<String> permissions = membership.getRole().getPermissions().stream()
                .map(PermissionEntity::getPermissionKey)
                .collect(Collectors.toUnmodifiableSet());
        return new AuthUserResponse(
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getUser().getDisplayName(),
                membership.getOrganization().getId(),
                membership.getOrganization().getName(),
                membership.getOrganization().getSlug(),
                membership.getRole().getName(),
                permissions);
    }

    private static byte[] passwordBytes(String password) {
        return password.getBytes(StandardCharsets.UTF_8);
    }

    public record IssuedSession(
            String refreshToken,
            AuthSessionResponse response,
            RefreshTokenEntity tokenRecord) {
    }
}
