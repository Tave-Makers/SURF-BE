package com.tavemakers.surf.presentation.letter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.tavemakers.surf.domain.letter.entity.Letter;

import java.time.LocalDateTime;

@Schema(description = "쪽지 조회 응답 DTO")
public record LetterResDTO(

        @Schema(description = "쪽지 ID", example = "12")
        Long letterId,

        @Schema(description = "쪽지 제목", example = "문의드립니다.")
        String title,

        @Schema(description = "쪽지 본문 내용", example = "안녕하세요, 몇 가지 문의 사항이 있습니다.")
        String content,

        @Schema(description = "추가 연락 SNS (선택사항)", example = "@instagram_user")
        String sns,

        @Schema(description = "발신자가 회신받고 싶은 이메일", example = "sender@example.com")
        String replyEmail,

        @Schema(description = "발신자 memberId", example = "1")
        Long senderId,

        @Schema(description = "발신자 이름", example = "임동규")
        String senderName,

        @Schema(description = "수신자 memberId", example = "3")
        Long receiverId,

        @Schema(description = "수신자 이름", example = "홍길동")
        String receiverName,

        @Schema(description = "쪽지 작성 시각", example = "2025-12-25T14:12:33")
        LocalDateTime createdAt

) {
    public static LetterResDTO from(Letter letter) {
        return new LetterResDTO(
                letter.getLetterId(),
                letter.getTitle(),
                letter.getContent(),
                letter.getSns(),
                letter.getReplyEmail(),
                letter.getSender().getId(),
                letter.getSender().getName(),
                letter.getReceiver().getId(),
                letter.getReceiver().getName(),
                letter.getCreatedAt()
        );
    }
}
