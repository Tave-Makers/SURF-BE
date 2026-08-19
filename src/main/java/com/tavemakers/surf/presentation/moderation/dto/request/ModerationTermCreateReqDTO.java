package com.tavemakers.surf.presentation.moderation.dto.request;

import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import com.tavemakers.surf.global.logging.LogPropsProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ModerationTermCreateReqDTO(

        @NotNull(message = "항목 종류(BANNED/ALLOWED)는 필수입니다.")
        ModerationTermType type,

        @NotBlank(message = "등록할 표현은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "등록할 표현은 100자를 넘을 수 없습니다.")
        String text

) implements LogPropsProvider {

        /** 감사 로그 props — 관리자 id·역할은 flush 시 공통 필드로 붙는다. */
        @Override
        public Map<String, Object> buildProps() {
                return Map.of(
                        "type", type.name(),
                        "text", text.trim()
                );
        }
}
