package com.tavemakers.surf.domain.letter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "쪽지 생성 요청 DTO")
public record LetterCreateReqDTO(

        @Schema(description = "수신자 memberId", example = "3")
        @NotNull
        Long receiverId,

        @Schema(description = "쪽지 제목", example = "문의드립니다.")
        @NotBlank
        @Size(max = 100)
        String title,

        @Schema(description = "쪽지 본문 내용", example = "안녕하세요, 몇 가지 질문이 있어 쪽지드립니다.")
        @NotBlank
        @Size(max = 10000)
        String content,

        @Schema(description = "추가 연락 SNS (선택사항)", example = "@instagram_user")
        @Size(max = 100)
        String sns,

        @Schema(description = "회신 받을 이메일", example = "sender@example.com")
        @NotBlank
        @Email
        @Size(max = 100)
        String replyEmail
) {}
