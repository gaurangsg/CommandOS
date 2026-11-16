package com.commandos.shared.api;

import java.time.Instant;

public record ApiErrorResponse(boolean success, ErrorDetail error, Instant timestamp) {

    public record ErrorDetail(String code, String message) {
    }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(false, new ErrorDetail(code, message), Instant.now());
    }
}
