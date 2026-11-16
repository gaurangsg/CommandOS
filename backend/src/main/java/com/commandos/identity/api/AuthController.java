package com.commandos.identity.api;

import com.commandos.identity.api.dto.AuthSessionResponse;
import com.commandos.identity.api.dto.AuthUserResponse;
import com.commandos.identity.api.dto.AcceptInvitationRequest;
import com.commandos.identity.api.dto.LoginRequest;
import com.commandos.identity.api.dto.RefreshRequest;
import com.commandos.identity.api.dto.RegisterRequest;
import com.commandos.identity.application.AuthService;
import com.commandos.people.api.dto.InvitationPreviewResponse;
import com.commandos.people.application.InvitationService;
import com.commandos.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "commandos_refresh";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final InvitationService invitationService;
    private final boolean secureCookie;
    private final Duration refreshLifetime;

    public AuthController(
            AuthService authService,
            InvitationService invitationService,
            @Value("${commandos.security.auth-cookie-secure}") boolean secureCookie,
            @Value("${commandos.security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.authService = authService;
        this.invitationService = invitationService;
        this.secureCookie = secureCookie;
        this.refreshLifetime = Duration.ofDays(refreshTokenDays);
    }

    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthSessionResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.IssuedSession issuedSession = authService.register(request);
        return sessionResponse(issuedSession, HttpServletResponse.SC_CREATED);
    }

    @PostMapping("/login")
    ResponseEntity<ApiResponse<AuthSessionResponse>> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(authService.login(request), HttpServletResponse.SC_OK);
    }

    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AuthSessionResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @Valid @RequestBody RefreshRequest request) {
        return sessionResponse(authService.refresh(refreshToken, request), HttpServletResponse.SC_OK);
    }

    @PostMapping("/logout")
    ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .body(ApiResponse.of(new LogoutResponse("logged_out")));
    }

    @GetMapping("/invitations/preview")
    ResponseEntity<ApiResponse<InvitationPreviewResponse>> invitationPreview(@RequestParam String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.of(invitationService.preview(token)));
    }

    @PostMapping("/invitations/accept")
    ResponseEntity<ApiResponse<AuthSessionResponse>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request) {
        AuthService.IssuedSession session = invitationService.accept(
                request.token(), request.displayName(), request.password());
        return sessionResponse(session, HttpServletResponse.SC_CREATED);
    }

    @GetMapping("/me")
    ApiResponse<AuthUserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(userFrom(jwt));
    }

    private ResponseEntity<ApiResponse<AuthSessionResponse>> sessionResponse(
            AuthService.IssuedSession session,
            int status) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), refreshLifetime).toString())
                .body(ApiResponse.of(session.response()));
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    public static AuthUserResponse userFrom(Jwt jwt) {
        return new AuthUserResponse(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                UUID.fromString(jwt.getClaimAsString("org_id")),
                jwt.getClaimAsString("org_name"),
                jwt.getClaimAsString("org_slug"),
                jwt.getClaimAsString("role"),
                Set.copyOf(jwt.getClaimAsStringList("permissions")));
    }

    public record LogoutResponse(String status) {
    }
}
