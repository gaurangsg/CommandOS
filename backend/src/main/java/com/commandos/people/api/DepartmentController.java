package com.commandos.people.api;

import com.commandos.identity.api.AuthController;
import com.commandos.identity.api.dto.AuthUserResponse;
import com.commandos.people.api.dto.DepartmentRequest;
import com.commandos.people.api.dto.DepartmentResponse;
import com.commandos.people.application.DepartmentService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('departments:read')")
    ApiResponse<List<DepartmentResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        return ApiResponse.of(departmentService.list(currentUser.organizationId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('departments:manage')")
    ApiResponse<DepartmentResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentRequest request) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        return ApiResponse.of(departmentService.create(currentUser.organizationId(), request));
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasAuthority('departments:manage')")
    ApiResponse<DepartmentResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID departmentId,
            @Valid @RequestBody DepartmentRequest request) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        return ApiResponse.of(departmentService.update(currentUser.organizationId(), departmentId, request));
    }

    @DeleteMapping("/{departmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('departments:manage')")
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID departmentId) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        departmentService.delete(currentUser.organizationId(), departmentId);
    }
}
