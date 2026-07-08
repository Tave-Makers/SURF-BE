package com.tavemakers.surf.domain.letter.domain.service;

import com.tavemakers.surf.domain.letter.domain.entity.Letter;
import com.tavemakers.surf.domain.letter.domain.event.LetterSentEvent;
import com.tavemakers.surf.domain.letter.domain.repository.LetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
