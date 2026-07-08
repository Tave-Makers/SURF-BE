package com.tavemakers.surf.domain.letter.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 쪽지 발송 이벤트 - 쪽지 수신자에게 알림 발송용
 */
@Getter
@RequiredArgsConstructor
public class LetterSentEvent {
    private final Long receiverId;      // 알림 받을 사람 (쪽지 수신자)
    private final String senderName;    // 쪽지 발신자 이름
    private final Long senderId;        // 쪽지 발신자 ID
}
