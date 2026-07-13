package com.tavemakers.surf.presentation.feedback.dto.request;

import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "피드백 생성 요청 DTO")
public record FeedbackCreateReqDTO(

        @Schema(description = "피드백 본문 내용", example = "열심히 일해주세요 ㅜ.ㅜ")
        @NotBlank String content
) implements LogPropsProvider {

        @Override
        public Map<String, Object> buildProps() {
                return Map.of(
                        "text_length", content != null ? content.length() : 0
                );
        }
}