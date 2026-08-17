package com.tavemakers.surf.application.moderation.usecase;

import com.tavemakers.surf.domain.moderation.entity.ModerationTerm;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import com.tavemakers.surf.domain.moderation.event.ModerationDictionaryChangedEvent;
import com.tavemakers.surf.domain.moderation.exception.ModerationTermDuplicateException;
import com.tavemakers.surf.domain.moderation.exception.ModerationTermNotFoundException;
import com.tavemakers.surf.domain.moderation.repository.ModerationTermRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventContext;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.presentation.moderation.dto.request.ModerationTermCreateReqDTO;
import com.tavemakers.surf.presentation.moderation.dto.response.ModerationTermResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * 금칙어 사전 Usecase — 트랜잭션 경계를 소유하고 편집 결과를 표현형(DTO)으로 매핑한다.
 * 편집은 커밋 이후 스냅숏 리빌드가 일어나도록 사전 변경 이벤트를 발행한다.
 *
 * <p>사전이 DB로 옮겨지면서 사라진 Git 이력을 대신해 등록·삭제를 전부 로깅한다.
 */
@Service
@RequiredArgsConstructor
public class ModerationTermUsecase {

    private final ModerationTermRepository moderationTermRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 사전 항목 등록 — 같은 (종류, 표현)이 이미 있으면 중복 예외 */
    @Transactional
    @LogEvent(value = "moderation.term.created", message = "금칙어 사전 항목 등록 성공")
    public ModerationTermResDTO createTerm(ModerationTermCreateReqDTO req) {
        String text = req.text().trim();

        if (moderationTermRepository.existsByTypeAndText(req.type(), text)) {
            throw new ModerationTermDuplicateException();
        }

        ModerationTerm saved = moderationTermRepository.save(ModerationTerm.of(req.type(), text));
        eventPublisher.publishEvent(ModerationDictionaryChangedEvent.from(saved));

        return ModerationTermResDTO.from(saved);
    }

    /** 사전 항목 삭제 */
    @Transactional
    @LogEvent(value = "moderation.term.deleted", message = "금칙어 사전 항목 삭제 성공")
    public void deleteTerm(@LogParam("term_id") Long termId) {
        ModerationTerm term = moderationTermRepository.findById(termId)
                .orElseThrow(ModerationTermNotFoundException::new);

        // 삭제된 항목이 무엇이었는지 감사 로그에 남긴다 — id만으로는 사후 추적이 불가능하다
        LogEventContext.put("type", term.getType().name());
        LogEventContext.put("text", term.getText());

        moderationTermRepository.delete(term);
        eventPublisher.publishEvent(ModerationDictionaryChangedEvent.from(term));
    }

    /**
     * 사전이 비어 있을 때만 시드 항목을 일괄 적재하고 적재 건수를 반환한다.
     * 비어 있는지 확인과 적재를 한 트랜잭션에 묶어 재기동 시 중복 적재를 막는다(멱등).
     */
    @Transactional
    public int seedIfEmpty(Collection<String> bannedWords, Collection<String> allowedPhrases) {
        if (moderationTermRepository.count() > 0) {
            return 0;
        }

        List<ModerationTerm> terms = Stream.concat(
                        toTerms(ModerationTermType.BANNED, bannedWords),
                        toTerms(ModerationTermType.ALLOWED, allowedPhrases))
                .toList();

        return moderationTermRepository.saveAll(terms).size();
    }

    private Stream<ModerationTerm> toTerms(ModerationTermType type, Collection<String> texts) {
        return texts.stream().map(text -> ModerationTerm.of(type, text));
    }

}
