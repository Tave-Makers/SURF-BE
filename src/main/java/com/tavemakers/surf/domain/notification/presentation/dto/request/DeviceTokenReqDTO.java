package com.tavemakers.surf.domain.notification.presentation.dto.request;

import com.tavemakers.surf.domain.notification.domain.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceTokenReqDTO(
        @NotBlank(message = "토큰은 필수입니다.")
        String token,

        @NotNull(message = "플랫폼은 필수입니다.")
        Platform platform
) {
}