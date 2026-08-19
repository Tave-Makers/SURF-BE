package com.tavemakers.surf.global.common.moderation;

import com.tavemakers.surf.global.common.loader.ModerationSeedLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 성능 회귀 — 10KB 본문 × 1,000회 마스킹.
 *
 * <p>실사전(588개)으로 트라이를 1회 구성한 뒤 본문만 반복 순회한다. 상한은 CI 변동을 견디도록
 * 넉넉히 잡았다 — 알고리즘이 O(패턴수 × 본문길이)로 퇴화하는 회귀를 잡는 것이 목적이지
 * 절대 성능을 재는 것이 목적이 아니다.
 */
class ProfanityMaskerPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(ProfanityMaskerPerformanceTest.class);

    private static final int ITERATIONS = 1_000;
    private static final int WARMUP = 50;
    private static final long LIMIT_MILLIS = 10_000L;

    @Test
    @DisplayName("10KB 본문 1,000회 마스킹이 넉넉한 상한(10초) 안에 끝난다")
    void 대용량_본문_반복_마스킹() {
        ProfanityMasker masker = new ProfanityMasker(new ModerationProperties(true, "*"));
        masker.replaceSnapshot(DictionarySnapshot.of(
                ModerationSeedLoader.readTerms("moderation/badwords.txt"),
                ModerationSeedLoader.readTerms("moderation/allowlist.txt")));

        String body = buildBody();
        assertThat(body.length()).isGreaterThanOrEqualTo(10_000); // 어떤 인코딩으로도 10KB 이상

        for (int i = 0; i < WARMUP; i++) {
            masker.mask(body);
        }

        long startedAt = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            masker.mask(body);
        }
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;

        log.info("[MODERATION] 마스킹 성능 — 본문 {}자 × {}회 = {}ms (평균 {}ms/회)",
                body.length(), ITERATIONS, elapsedMillis, (double) elapsedMillis / ITERATIONS);

        assertThat(elapsedMillis).isLessThan(LIMIT_MILLIS);
    }

    /** 정상 게시글에 금칙어가 드문드문 섞인 10KB(1만 자) 본문 — 치환 경로까지 함께 태운다. */
    private String buildBody() {
        String paragraph = "이번 주 세션 발표 자료를 공유드립니다. 개발 환경 세팅 가이드는 노션에 정리해두었고, "
                + "분석 파트는 아직 결과를 보지 못했어요. 마스터 브랜치에 머지한 뒤 배포하겠습니다. "
                + "질문은 댓글로 남겨주세요. 씨발 이번 일정은 진짜 빡세네요. ";
        return paragraph.repeat(10_000 / paragraph.length() + 1);
    }
}
