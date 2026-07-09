package com.tavemakers.surf.presentation.feedback.dto.response;

import com.tavemakers.surf.domain.feedback.entity.Feedback;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "피드백 응답 DTO")
public record FeedbackResDTO(

        @Schema(description = "피드백 ID", example = "1")
        Long id,

        @Schema(description = "피드백 본문 내용", example = "열심히 일해주세요 ㅜ.ㅜ")
        String content,

        @Schema(description = "피드백 생성 일시", example = "2023-10-05T14:48:00")
        LocalDateTime createdAt
) {
    public static FeedbackResDTO from(Feedback f) {
        return new FeedbackResDTO(
                f.getId(),
                f.getContent(),
                f.getCreatedAt()
        );
    }
}
