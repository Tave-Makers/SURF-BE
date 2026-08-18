package com.tavemakers.surf.application.moderation.service;

import com.tavemakers.surf.domain.moderation.entity.ModerationTerm;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import com.tavemakers.surf.domain.moderation.repository.ModerationTermRepository;
import com.tavemakers.surf.global.common.moderation.DictionarySnapshot;
import com.tavemakers.surf.global.common.moderation.ProfanityMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사전 스냅숏 리빌드 — DB의 금칙어·허용 표현 전체를 읽어 엔진 스냅숏을 통째로 교체한다.
 * 시드 로더(기동)·갱신 리스너(편집)·폴링 스케줄러(타 인스턴스 전파)가 공유하는 단일 진입점이다.
 *
 * <p>커밋 이후(AFTER_COMMIT) 호출되는 경로가 있으므로 REQUIRES_NEW 로 자체 트랜잭션을 연다 —
 * 이미 완료된 트랜잭션에 편승하면 조회 시점이 모호해진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryReloader {

    private final ModerationTermRepository moderationTermRepository;
    private final ProfanityMasker profanityMasker;

    /** 사전 전체를 읽어 스냅숏을 교체하고 반영된 금칙어 수를 반환한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public int reload() {
        List<String> bannedWords = findTexts(ModerationTermType.BANNED);
        List<String> allowedPhrases = findTexts(ModerationTermType.ALLOWED);

        profanityMasker.replaceSnapshot(DictionarySnapshot.of(bannedWords, allowedPhrases));
        log.info("[MODERATION] 사전 스냅숏 갱신 — 금칙어 {}건, 허용 표현 {}건",
                bannedWords.size(), allowedPhrases.size());

        return bannedWords.size();
    }

    private List<String> findTexts(ModerationTermType type) {
        return moderationTermRepository.findAllByTypeOrderByTextAsc(type).stream()
                .map(ModerationTerm::getText)
                .toList();
    }

}
