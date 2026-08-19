package com.tavemakers.surf.application.moderation;

import com.tavemakers.surf.application.moderation.service.DictionaryReloader;
import com.tavemakers.surf.application.moderation.usecase.ModerationTermUsecase;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import com.tavemakers.surf.domain.moderation.repository.ModerationTermRepository;
import com.tavemakers.surf.e2e.E2ESupport;
import com.tavemakers.surf.global.common.moderation.ProfanityMasker;
import com.tavemakers.surf.presentation.moderation.dto.request.ModerationTermCreateReqDTO;
import com.tavemakers.surf.presentation.moderation.dto.response.ModerationTermResDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사전 갱신 전파 통합 테스트.
 *
 * <p>관리자 등록 → 커밋 → {@code @TransactionalEventListener(AFTER_COMMIT)} 리스너 → 스냅숏 리빌드까지
 * 실제 컨텍스트로 확인한다. 리스너가 {@code @Async} 라 반영은 즉시가 아니므로 짧게 폴링한다
 * (프로젝트에 Awaitility 의존성이 없어 폴링 루프를 쓴다).
 *
 * <p>테스트 트랜잭션을 열면 AFTER_COMMIT 이 발화하지 않으므로 {@code NOT_SUPPORTED} 로 비활성화한다.
 * 대신 커밋된 항목은 {@code @AfterEach} 에서 직접 정리한다 — 컨텍스트·H2 는 다른 테스트와 공유된다.
 */
class ModerationDictionaryPropagationTest extends E2ESupport {

    private static final String NEW_TERM = "테스트전파금칙어";
    private static final long TIMEOUT_MILLIS = 10_000L;
    private static final long POLL_INTERVAL_MILLIS = 50L;

    @Autowired
    private ModerationTermUsecase moderationTermUsecase;

    @Autowired
    private ModerationTermRepository moderationTermRepository;

    @Autowired
    private DictionaryReloader dictionaryReloader;

    @Autowired
    private ProfanityMasker profanityMasker;

    private Long createdTermId;

    @AfterEach
    void cleanUpCreatedTerm() {
        if (createdTermId != null) {
            moderationTermUsecase.deleteTerm(createdTermId);
            createdTermId = null;
        }
        dictionaryReloader.reload();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("기동 시드가 적재된 사전이 엔진 스냅숏에 반영돼 있다")
    void 기동_시드가_스냅숏에_반영된다() {
        assertThat(moderationTermRepository.count()).isGreaterThan(500L);
        assertThat(profanityMasker.mask("씨발 진짜")).isEqualTo("** 진짜");
        assertThat(profanityMasker.mask("아직 결과를 보지 못했어요")).isEqualTo("아직 결과를 보지 못했어요");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("관리자가 등록한 금칙어가 커밋 후 리스너를 거쳐 마스킹에 반영된다")
    void 관리자_등록이_AFTER_COMMIT_리스너로_전파된다() {
        String sentence = NEW_TERM + " 라고 했다";
        assertThat(profanityMasker.mask(sentence))
                .as("등록 전에는 사전에 없는 단어라 마스킹되지 않아야 한다")
                .isEqualTo(sentence);

        ModerationTermResDTO created = moderationTermUsecase.createTerm(
                new ModerationTermCreateReqDTO(ModerationTermType.BANNED, NEW_TERM));
        createdTermId = created.termId();

        String masked = awaitMasked(sentence);

        assertThat(masked).isEqualTo("*".repeat(NEW_TERM.length()) + " 라고 했다");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("관리자가 삭제한 금칙어는 커밋 후 마스킹 대상에서 빠진다 (오탐 즉시 대응 경로)")
    void 관리자_삭제가_AFTER_COMMIT_리스너로_전파된다() {
        String sentence = NEW_TERM + " 라고 했다";
        ModerationTermResDTO created = moderationTermUsecase.createTerm(
                new ModerationTermCreateReqDTO(ModerationTermType.BANNED, NEW_TERM));
        createdTermId = created.termId();
        awaitMasked(sentence);

        moderationTermUsecase.deleteTerm(created.termId());
        createdTermId = null;

        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!profanityMasker.mask(sentence).equals(sentence) && System.currentTimeMillis() < deadline) {
            sleepBriefly();
        }
        assertThat(profanityMasker.mask(sentence)).isEqualTo(sentence);
    }

    /** 비동기 리스너가 스냅숏을 갈아 끼울 때까지 폴링한다. */
    private String awaitMasked(String sentence) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        String masked = profanityMasker.mask(sentence);
        while (masked.equals(sentence) && System.currentTimeMillis() < deadline) {
            sleepBriefly();
            masked = profanityMasker.mask(sentence);
        }
        assertThat(masked)
                .as("AFTER_COMMIT 리스너가 %dms 안에 스냅숏을 갱신해야 한다", TIMEOUT_MILLIS)
                .isNotEqualTo(sentence);
        return masked;
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("폴링 대기 중 인터럽트", e);
        }
    }
}
