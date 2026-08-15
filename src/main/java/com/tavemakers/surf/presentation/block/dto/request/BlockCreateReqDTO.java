package com.tavemakers.surf.presentation.block.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "차단 등록 요청")
public record BlockCreateReqDTO(

        @Schema(description = "차단할 회원 ID", example = "12")
        @NotNull(message = "차단할 회원 ID는 필수입니다.")
        Long memberId
) {
}
