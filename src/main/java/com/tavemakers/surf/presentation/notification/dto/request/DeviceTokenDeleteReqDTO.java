package com.tavemakers.surf.presentation.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenDeleteReqDTO(
        @NotBlank(message = "토큰은 필수입니다.")
        String token
) {
}
