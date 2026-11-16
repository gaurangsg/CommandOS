package com.commandos.organization.api;

import com.commandos.identity.api.AuthController;
import com.commandos.identity.api.dto.AuthUserResponse;
import com.commandos.organization.api.dto.OrganizationResponse;
import com.commandos.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('organization:read')")
    ApiResponse<OrganizationResponse> current(@AuthenticationPrincipal Jwt jwt) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        return ApiResponse.of(new OrganizationResponse(
                currentUser.organizationId(),
                currentUser.organizationName(),
                currentUser.organizationSlug(),
                currentUser.role()));
    }
}
