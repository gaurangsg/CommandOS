package com.commandos.platform.health;

import com.commandos.shared.api.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void healthResponseIdentifiesRunningApi() {
        ApiResponse<HealthController.HealthStatus> response = controller.health();

        assertThat(response.success()).isTrue();
        assertThat(response.data().status()).isEqualTo("UP");
        assertThat(response.data().service()).isEqualTo("CommandOS API");
        assertThat(response.timestamp()).isNotNull();
    }
}
