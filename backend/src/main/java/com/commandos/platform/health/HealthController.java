package com.commandos.platform.health;

import com.commandos.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthStatus> health() {
        return ApiResponse.of(new HealthStatus("UP", "CommandOS API"));
    }

    public record HealthStatus(String status, String service) {
    }
}
