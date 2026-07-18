package com.tavemakers.surf.domain.letter.event;

/** 쪽지 저장 커밋 후 수신자에게 이메일을 발송하기 위한 이벤트 (리스너: application/letter/event) */
public record LetterEmailRequestedEvent(
        Long letterId,
        Long senderId,
        Long receiverId,
        String senderName,
        String receiverEmail,
        String title,
        String content,
        String replyEmail,
        String sns
) {
}
