package com.lootsafe.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}