package com.tavemakers.surf.application.letter.usecase;

import com.tavemakers.surf.domain.letter.entity.Letter;
import com.tavemakers.surf.domain.letter.event.LetterEmailRequestedEvent;
import com.tavemakers.surf.domain.letter.event.LetterSentEvent;
import com.tavemakers.surf.domain.letter.repository.LetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쪽지 저장 트랜잭션 경계를 소유하는 application 서비스.
 * 저장과 이벤트 발행을 하나의 트랜잭션으로 커밋하면, AFTER_COMMIT 리스너가
 * 알림(FCM)과 이메일 발송을 비동기로 수행한다. LetterUsecase와 분리된
 * 별도 빈이어야 한다 — self-invocation 프록시 우회를 위함.
 */
@Service
@RequiredArgsConstructor
public class LetterCreateService {

    private final LetterRepository letterRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 쪽지 저장 및 알림·이메일 이벤트 발행 (AFTER_COMMIT 리스너가 커밋 후 발화) */
    @Transactional
    public Letter save(Letter letter) {
        Letter saved = letterRepository.save(letter);
        eventPublisher.publishEvent(new LetterSentEvent(
                saved.getReceiver().getId(),
                saved.getSender().getName(),
                saved.getSender().getId()
        ));
        eventPublisher.publishEvent(new LetterEmailRequestedEvent(
                saved.getLetterId(),
                saved.getSender().getId(),
                saved.getReceiver().getId(),
                saved.getSender().getName(),
                saved.getReceiver().getEmail(),
                saved.getTitle(),
                saved.getContent(),
                saved.getReplyEmail(),
                saved.getSns()
        ));
        return saved;
    }
}
