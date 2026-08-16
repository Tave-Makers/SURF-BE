package com.tavemakers.surf.application.letter.event;

import com.tavemakers.surf.domain.letter.event.LetterEmailRequestedEvent;
import com.tavemakers.surf.global.util.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 쪽지 이메일 발송 리스너 — 커밋 후 비동기로 SMTP 발송을 수행한다.
 * <p>기존에는 요청 스레드에서 동기 발송해 응답이 SMTP 왕복(~3초)만큼 지연됐다.
 * 발송 실패는 응답에 영향을 주지 않으며 서버 로그로만 남긴다(쪽지 저장은 이미 커밋됨).
 * LogEventEmitter는 요청 스레드 ThreadLocal 기반이라 비동기 스레드에서는 slf4j로 기록한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LetterEmailListener {

    private final EmailSender emailSender;

    /** 쪽지 이메일 발송 (AFTER_COMMIT + 비동기) */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LetterEmailRequestedEvent event) {
        String body = """
        [Surf에서 %s님이 보낸 쪽지입니다.]

        %s

        회신 희망 이메일: %s
        SNS: %s
        """
                .formatted(
                        event.senderName(),
                        event.content(),
                        event.replyEmail(),
                        event.sns() != null ? event.sns() : "-"
                );

        try {
            emailSender.sendMail(event.receiverEmail(), event.title(), body);
            log.info("[LetterEmail] sent letterId={} senderId={} receiverId={}",
                    event.letterId(), event.senderId(), event.receiverId());
        } catch (MailException e) {
            log.error("[LetterEmail] send failed letterId={} senderId={} receiverId={} - {}",
                    event.letterId(), event.senderId(), event.receiverId(), e.getMessage());
        }
    }
}
