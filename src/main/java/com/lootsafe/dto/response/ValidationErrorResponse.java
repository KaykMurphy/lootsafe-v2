package com.lootsafe.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ValidationErrorResponse(
        int status,
        String error,
        String message,
        List<FieldErrorDetail> errors,
        String path,
        OffsetDateTime timestamp
) {
    public record FieldErrorDetail(String field, String message) {}

    public ValidationErrorResponse(int status, String error, String message, List<FieldErrorDetail> errors, String path) {
        this(status, error, message, errors, path, OffsetDateTime.now());
    }
}