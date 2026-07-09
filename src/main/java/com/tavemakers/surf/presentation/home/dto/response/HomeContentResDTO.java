package com.tavemakers.surf.presentation.home.dto.response;

import com.tavemakers.surf.domain.home.entity.HomeContent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;


@Builder
public record HomeContentResDTO(

        @Schema(description = "홈 문구 ID", example = "1")
        Long id,

        @Schema(description = "홈 문구", example = "TAVE 신규 회원을 환영합니다.")
        String message,

        @Schema(description = "홈 문구 작성자", example = "TAVE 운영진")
        String sender
) {
    public static HomeContentResDTO from(HomeContent hc) {
        return HomeContentResDTO.builder()
                .id(hc.getId())
                .message(hc.getMessage())
                .sender(hc.getSender())
                .build();
    }
}