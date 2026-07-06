package com.tavemakers.surf.support;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 동시성 버그 재현용 공용 헬퍼.
 *
 * 사용 예:
 * <pre>
 * ConcurrencyTestHelper.Result result =
 *         ConcurrencyTestHelper.runConcurrently(10, () -> commentLikeService.toggleLike(commentId, memberId));
 * assertThat(result.successCount()).isEqualTo(10);
 * assertThat(comment.getLikeCount()).isEqualTo(expected); // lost update 검증
 * </pre>
 */
public final class ConcurrencyTestHelper {

    private ConcurrencyTestHelper() {
    }

    /** threadCount개의 스레드가 latch로 동시에 task를 시작하고, 전부 끝날 때까지 대기한다. */
    public static Result runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run();
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown(); // 전 스레드 동시 출발
        boolean finished = done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        if (!finished) {
            throw new IllegalStateException("30초 내에 모든 스레드가 종료되지 않음 (데드락 의심)");
        }
        return new Result(successCount.get(), failures);
    }

    public record Result(int successCount, List<Throwable> failures) {

        public int failureCount() {
            return failures.size();
        }
    }
}
