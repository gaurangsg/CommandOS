package com.commandos.identity.application;

import com.commandos.identity.api.dto.LoginRequest;
import com.commandos.identity.api.dto.RefreshRequest;
import com.commandos.identity.api.dto.RegisterRequest;
import com.commandos.identity.domain.RefreshTokenEntity;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AuthServiceTest {

    private UserRepository userRepository;
    private OrganizationRepository organizationRepository;
    private PermissionRepository permissionRepository;
    private RoleRepository roleRepository;
    private MembershipRepository membershipRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService jwtTokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        roleRepository = mock(RoleRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenService = mock(JwtTokenService.class);
        authService = new AuthService(
                userRepository,
                organizationRepository,
                permissionRepository,
                roleRepository,
                membershipRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenService,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                30);
    }

    @Test
    void loginUsesSameGenericErrorForInvalidCredentials() {
        MembershipEntity membership = mock(MembershipEntity.class);
        var user = mock(com.commandos.identity.domain.UserEntity.class);
        var organization = mock(OrganizationEntity.class);
        var role = mock(RoleEntity.class);
        when(membershipRepository.findForAuthentication("alex@example.com", "northstar"))
                .thenReturn(Optional.of(membership));
        when(membership.getUser()).thenReturn(user);
        when(membership.getOrganization()).thenReturn(organization);
        when(membership.getRole()).thenReturn(role);
        when(user.getStatus()).thenReturn("ACTIVE");
        when(user.getPasswordHash()).thenReturn("password-hash");
        when(membership.getStatus()).thenReturn("ACTIVE");
        when(organization.getStatus()).thenReturn("ACTIVE");
        when(passwordEncoder.matches("wrong-password", "password-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("Alex@example.com", "wrong-password", "Northstar")))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.getMessage())
                            .isEqualTo("Email, password, or workspace is incorrect.");
                });
        verify(jwtTokenService, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registrationRejectsPasswordsLongerThanBcryptByteLimit() {
        RegisterRequest request = new RegisterRequest(
                "Alex Morgan",
                "alex@example.com",
                "é".repeat(37),
                "Northstar Studio",
                "northstar");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception ->
                        assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void registrationGrantsOwnerPeopleAndDepartmentPermissions() {
        List<String> permissionKeys = List.of(
                "organization:read", "organization:manage", "members:read", "members:manage",
                "roles:manage", "audit:read", "people:read", "people:manage",
                "departments:read", "departments:manage");
        List<PermissionEntity> permissions = new ArrayList<>();
        for (String key : permissionKeys) {
            PermissionEntity permission = mock(PermissionEntity.class);
            when(permission.getPermissionKey()).thenReturn(key);
            permissions.add(permission);
        }
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);
        when(permissionRepository.findAllByPermissionKeyIn(anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<String> requested = invocation.getArgument(0);
                    return permissions.stream()
                            .filter(permission -> requested.contains(permission.getPermissionKey()))
                            .toList();
                });
        when(organizationRepository.saveAndFlush(any(OrganizationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.saveAndFlush(any(com.commandos.identity.domain.UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("password-hash");
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(MembershipEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenService.create(any())).thenReturn("access-token");
        when(jwtTokenService.getAccessTokenSeconds()).thenReturn(900L);

        AuthService.IssuedSession session = authService.register(new RegisterRequest(
                "Alex Morgan",
                "alex@example.com",
                "correct-horse-battery",
                "Northstar Studio",
                "northstar"));

        assertThat(session.response().user().permissions())
                .contains("people:read", "people:manage", "departments:read", "departments:manage");
    }

    @Test
    void refreshTokenReplayRevokesTheActiveTokenFamily() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UUID familyId = UUID.randomUUID();
        var user = mock(com.commandos.identity.domain.UserEntity.class);
        RefreshTokenEntity replayedToken = new RefreshTokenEntity(
                UUID.randomUUID(), familyId, user, new byte[32], now.plusSeconds(60));
        RefreshTokenEntity replacementToken = new RefreshTokenEntity(
                UUID.randomUUID(), familyId, user, new byte[32], now.plusSeconds(120));
        replayedToken.revoke(now.minusSeconds(1), replacementToken);
        when(refreshTokenRepository.findByTokenHashForUpdate(any(byte[].class)))
                .thenReturn(Optional.of(replayedToken));

        assertThatThrownBy(() -> authService.refresh(
                "replayed-refresh-token", new RefreshRequest("northstar")))
                .isInstanceOf(ApiException.class)
                .satisfies(exception ->
                        assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(refreshTokenRepository).revokeActiveFamily(eq(familyId), eq(now));
    }
}
