package com.tavemakers.surf.global.common.moderation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스냅숏 교체 원자성 스모크.
 *
 * <p>마스킹이 진행되는 동안 {@code replaceSnapshot} 이 반복 호출돼도, 각 호출의 결과는
 * 교체 전 사전 또는 교체 후 사전 중 <b>하나로 일관</b>돼야 한다. 두 사전이 섞인 중간 결과가
 * 나오면 volatile 스냅숏 통째 교체 전제가 깨진 것이다.
 */
class ProfanityMaskerSnapshotSwapTest {

    private static final String TEXT = "씨발 잼민이 왔다";
    private static final String MASKED_BY_OLD = "** 잼민이 왔다";
    private static final String MASKED_BY_NEW = "씨발 *** 왔다";

    @Test
    @DisplayName("마스킹 도중 스냅숏이 교체돼도 결과는 두 사전 중 하나로 일관된다")
    void 스냅숏_교체_중에도_결과가_일관된다() throws InterruptedException {
        ProfanityMasker masker = new ProfanityMasker(new ModerationProperties(true, "*"));
        DictionarySnapshot oldSnapshot = DictionarySnapshot.of(List.of("씨발"), List.of());
        DictionarySnapshot newSnapshot = DictionarySnapshot.of(List.of("잼민이"), List.of());
        masker.replaceSnapshot(oldSnapshot);

        Set<String> results = ConcurrentHashMap.newKeySet();
        CountDownLatch maskersDone = new CountDownLatch(8);
        CountDownLatch allDone = new CountDownLatch(9);
        ExecutorService executor = Executors.newFixedThreadPool(9);

        // 교체 스레드는 마스킹이 끝날 때까지 계속 사전을 갈아 끼운다.
        executor.submit(() -> {
            try {
                int i = 0;
                while (maskersDone.getCount() > 0) {
                    masker.replaceSnapshot(i++ % 2 == 0 ? newSnapshot : oldSnapshot);
                }
            } finally {
                allDone.countDown();
            }
        });
        for (int i = 0; i < 8; i++) {
            executor.submit(() -> {
                try {
                    for (int n = 0; n < 5_000; n++) {
                        results.add(masker.mask(TEXT));
                    }
                } finally {
                    maskersDone.countDown();
                    allDone.countDown();
                }
            });
        }

        boolean finished = allDone.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(finished).as("30초 안에 모든 스레드가 종료돼야 한다").isTrue();
        assertThat(results)
                .as("사전이 섞인 중간 결과가 나오면 안 된다")
                .isSubsetOf(MASKED_BY_OLD, MASKED_BY_NEW);
        assertThat(results).isNotEmpty();
    }
}
