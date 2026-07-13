package com.tavemakers.surf.application.reservation.task;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.application.post.query.PostFileGetService;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.application.post.query.PostImageGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.service.post.PostPublishService;
import com.tavemakers.surf.domain.post.service.support.ViewCountService;
import com.tavemakers.surf.domain.reservation.entity.Reservation;
import com.tavemakers.surf.domain.reservation.entity.ReservationStatus;
import com.tavemakers.surf.domain.reservation.exception.ReservationNotFoundException;
import com.tavemakers.surf.application.reservation.query.ReservationGetService;
import com.tavemakers.surf.application.scrap.query.ScrapGetService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 예약 발행(PostPublishRunner)의 발행 race 회귀 테스트.
 *
 * <p>수정 전 문제: 발행 시각의 Runner 트랜잭션이 게시글을 무잠금 로드하고 상태 가드 없이
 * post.publish() 를 호출했다. 예약 변경(updateReservationPost) 트랜잭션이 락 대기 중인
 * Runner 와 겹치면, 방금 취소된 예약이 그대로 발행되어(재예약한 나중 시각을 무시)
 * 게시글이 잘못 공개되고 발행 알림이 중복 발송됐다.
 *
 * <p>수정 후 기대 동작: Runner 는 updateReservationPost 와 같은 순서(post 행 락 → reservation)로
 * 게시글 행 락을 먼저 잡고, 락 획득 후 예약 최신 상태를 잠금 읽기로 재검증한다. 락 대기 중
 * 취소·재예약된 예약이면 발행하지 않고(no-op), 이미 발행된 게시글도 멱등 no-op 이다.
 *
 * <p>진짜 동시 재현(취소 커밋이 Runner 의 스냅샷 읽기와 락 획득 사이에 끼어드는 창)은
 * 게시글 행 락을 선점한 블로커 트랜잭션으로 순서를 고정해 결정적으로 재현한다
 * (락 해제 시점 = 취소 커밋 시점이므로 인터리빙이 보장된다).
 *
 * <p>각 스레드가 독립 트랜잭션으로 커밋해야 하므로 클래스 트랜잭션을 NOT_SUPPORTED 로
 * 비활성화한다 (ReservationUpdateConcurrencyTest 와 동일 패턴).
 */
@DataJpaTest
@Import({
        PostPublishRunner.class,
        ReservationGetService.class,
        PostGetService.class,
        PostPublishService.class,
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostPublishRunnerRaceTest {

    @Autowired
    private PostPublishRunner publishRunner;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // PostGetService 의 잠금 조회 외 부가 의존성 — 이 테스트에서는 호출되지 않는다.
    @MockBean
    private ScrapGetService scrapGetService;
    @MockBean
    private PostLikeService postLikeService;
    @MockBean
    private PostImageGetService postImageGetService;
    @MockBean
    private PostFileGetService postFileGetService;
    @MockBean
    private ViewCountService viewCountService;

    private Long postId;
    private Long reservationId;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Board board = Board.builder().name("자유게시판").type(BoardType.GENERAL).build();
            entityManager.persist(board);

            BoardCategory category = BoardCategory.builder()
                    .board(board).name("잡담").slug("chat").build();
            entityManager.persist(category);

            Member author = persistMember("author" + System.nanoTime() + "@test.com");

            Post post = Post.builder()
                    .title("제목").content("내용")
                    .board(board).boardName(board.getName())
                    .category(category).categoryName(category.getName())
                    .member(author)
                    .postedAt(LocalDateTime.now().minusDays(1))
                    .isReserved(true)
                    .build();
            entityManager.persist(post);

            Reservation reservation = Reservation.of(post.getId(), Instant.now().plusSeconds(3600));
            entityManager.persist(reservation);

            this.postId = post.getId();
            this.reservationId = reservation.getId();
        });
    }

    private Member persistMember(String email) {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(System.nanoTime()))
                .kakaoId(System.nanoTime())
                .name("회원")
                .email(email)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        entityManager.persist(member);
        return member;
    }

    @Test
    @DisplayName("이미 취소된 예약으로 Runner가 실행되면 게시글은 발행되지 않는다")
    void 취소된_예약은_발행되지_않는다() {
        // 예약 취소 커밋 (Runner 실행 전 — 발행 전 취소 가드)
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status ->
                entityManager.find(Reservation.class, reservationId).cancel());

        assertThatThrownBy(() -> publishRunner.publishPost(reservationId))
                .isInstanceOf(ReservationNotFoundException.class);

        assertThat(findPost().isReserved())
                .as("취소된 예약으로는 게시글이 발행되면 안 된다")
                .isTrue();
    }

    @Test
    @DisplayName("Runner가 게시글 락 대기 중에 예약이 취소·재예약되면 발행하지 않는다 (race 창)")
    void 락_대기중_취소된_예약은_발행되지_않는다() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch blockerLocked = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // 예약 변경 트랜잭션 역할: 게시글 행 락을 선점한 채 대기하다가,
            // Runner 가 락 대기에 들어간 뒤 예약을 취소 + 재예약(R2)하고 커밋한다.
            Future<?> blocker = executor.submit(() -> tx.executeWithoutResult(status -> {
                entityManager.find(Post.class, postId, LockModeType.PESSIMISTIC_WRITE);
                blockerLocked.countDown();
                try {
                    releaseBlocker.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                entityManager.find(Reservation.class, reservationId).cancel();
                entityManager.persist(Reservation.of(postId, Instant.now().plusSeconds(7200)));
            }));

            assertThat(blockerLocked.await(5, TimeUnit.SECONDS)).isTrue();

            // Runner: 예약 스냅샷(RESERVED) 읽기 통과 → 게시글 행 락 대기(블로킹)
            Future<?> runnerFuture = executor.submit(() -> publishRunner.publishPost(reservationId));

            Thread.sleep(300); // Runner 가 예약 읽기를 지나 락 대기에 도달할 시간
            releaseBlocker.countDown(); // 취소·재예약 커밋 → 락 해제 → Runner 재개

            blocker.get(10, TimeUnit.SECONDS);
            // 수정 전에는 Runner 가 취소된 예약을 그대로 발행했다. 수정 후에는 no-op 으로 정상 종료한다.
            runnerFuture.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(findPost().isReserved())
                .as("락 대기 중 취소된 예약이 발행되면 재예약한 나중 시각을 무시하고 게시글이 공개된다")
                .isTrue();
        assertThat(findReservationStatus(reservationId))
                .as("취소된 예약이 PUBLISHED 로 덮어써지면 안 된다")
                .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(countByStatus(ReservationStatus.RESERVED))
                .as("재예약된 새 예약(R2)만 RESERVED 로 남아야 한다")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 발행된 게시글에 Runner가 다시 실행돼도 no-op이다 (멱등)")
    void 이미_발행된_게시글은_재실행해도_변경되지_않는다() {
        // 게시글을 먼저 발행 상태로 전이 (isReserved=false, postedAt 갱신)
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> entityManager.find(Post.class, postId).publish());
        LocalDateTime postedAtBefore = findPost().getPostedAt();

        // 수정 전에는 post.publish() 가 무가드로 재실행되어 postedAt 이 덮어써지고 알림이 중복 발송됐다.
        assertThatCode(() -> publishRunner.publishPost(reservationId))
                .doesNotThrowAnyException();

        Post post = findPost();
        assertThat(post.isReserved()).isFalse();
        assertThat(post.getPostedAt())
                .as("이미 발행된 게시글의 발행 시각이 재실행으로 덮어써지면 안 된다")
                .isEqualTo(postedAtBefore);
    }

    /** 게시글을 새 읽기 트랜잭션에서 조회한다. */
    private Post findPost() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(Post.class, postId));
    }

    private ReservationStatus findReservationStatus(Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(Reservation.class, id).getStatus());
    }

    private Long countByStatus(ReservationStatus status) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(txStatus -> entityManager
                .createQuery("select count(r) from Reservation r where r.postId = :pid and r.status = :status", Long.class)
                .setParameter("pid", postId)
                .setParameter("status", status)
                .getSingleResult());
    }
}
