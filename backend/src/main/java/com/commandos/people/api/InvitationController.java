package com.commandos.people.api;

import com.commandos.identity.api.AuthController;
import com.commandos.identity.api.dto.AuthUserResponse;
import com.commandos.people.api.dto.InviteEmployeeRequest;
import com.commandos.people.api.dto.InvitationResponse;
import com.commandos.people.application.InvitationService;
import com.commandos.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/people/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('people:manage')")
    ApiResponse<List<InvitationResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        return ApiResponse.of(invitationService.list(currentUser.organizationId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('people:manage')")
    ApiResponse<InvitationResponse> invite(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InviteEmployeeRequest request) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        return ApiResponse.of(invitationService.invite(
                currentUser.organizationId(), currentUser.userId(), request));
    }

    @DeleteMapping("/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('people:manage')")
    void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invitationId) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        invitationService.revoke(currentUser.organizationId(), invitationId);
    }
}
