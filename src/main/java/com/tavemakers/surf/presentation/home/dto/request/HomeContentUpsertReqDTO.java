package com.tavemakers.surf.presentation.home.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HomeContentUpsertReqDTO(

        @Schema(description = "홈 문구", example = "TAVE 신규 회원을 환영합니다.")
        @NotBlank @Size(max = 2000)
        String message,

        @Schema(description = "홈 문구 작성자", example = "TAVE 운영진")
        @NotBlank @Size(max = 200)
        String sender
) {}