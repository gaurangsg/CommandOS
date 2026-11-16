package com.commandos.people.api;

import com.commandos.identity.api.AuthController;
import com.commandos.identity.api.dto.AuthUserResponse;
import com.commandos.people.api.dto.EmployeeProfileRequest;
import com.commandos.people.api.dto.EmployeeResponse;
import com.commandos.people.api.dto.PageResponse;
import com.commandos.people.application.PeopleService;
import com.commandos.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/people")
public class PeopleController {

    private final PeopleService peopleService;

    public PeopleController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('people:read')")
    ApiResponse<PageResponse<EmployeeResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) UUID departmentId) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        return ApiResponse.of(peopleService.listEmployees(
                currentUser.organizationId(), page, size, search, departmentId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('people:manage')")
    ApiResponse<EmployeeResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody EmployeeProfileRequest request) {
        AuthUserResponse currentUser = AuthController.userFrom(jwt);
        return ApiResponse.of(peopleService.updateProfile(currentUser.organizationId(), userId, request));
    }
}
