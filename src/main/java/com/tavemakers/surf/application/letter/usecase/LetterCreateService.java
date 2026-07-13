package com.tavemakers.surf.application.letter.usecase;

import com.tavemakers.surf.domain.letter.entity.Letter;
import com.tavemakers.surf.domain.letter.event.LetterSentEvent;
import com.tavemakers.surf.domain.letter.repository.LetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쪽지 저장 트랜잭션 경계를 소유하는 application 서비스.
 * 저장·이벤트 발행을 하나의 트랜잭션으로 커밋한 뒤, 호출자(LetterUsecase)가
 * 트랜잭션 밖에서 메일을 보낸다(저장후-메일 패턴). EmailSender를 쥔 LetterUsecase와
 * 분리된 별도 빈이어야 한다 — self-invocation 프록시 우회 및 R5(트랜잭션 클래스의
 * 외부 클라이언트 의존 금지) 회피를 위함.
 */
@Service
@RequiredArgsConstructor
public class LetterCreateService {

    private final LetterRepository letterRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 쪽지 저장 및 발송 이벤트 발행 (AFTER_COMMIT 알림 리스너가 커밋 후 발화) */
    @Transactional
    public Letter save(Letter letter) {
        Letter saved = letterRepository.save(letter);
        eventPublisher.publishEvent(new LetterSentEvent(
                saved.getReceiver().getId(),
                saved.getSender().getName(),
                saved.getSender().getId()
        ));
        return saved;
    }
}
