package com.tavemakers.surf.domain.reservation.usecase;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.application.query.PostFileGetService;
import com.tavemakers.surf.domain.post.application.query.PostGetService;
import com.tavemakers.surf.domain.post.application.query.PostImageGetService;
import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.post.domain.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.domain.service.support.ViewCountService;
import com.tavemakers.surf.domain.reservation.entity.Reservation;
import com.tavemakers.surf.domain.reservation.entity.ReservationStatus;
import com.tavemakers.surf.domain.reservation.exception.ReservationAlreadyPublishedException;
import com.tavemakers.surf.domain.reservation.service.ReservationCreateService;
import com.tavemakers.surf.domain.reservation.service.ReservationGetService;
import com.tavemakers.surf.domain.reservation.service.ReservationScheduleService;
import com.tavemakers.surf.domain.scrap.application.query.ScrapGetService;
import com.tavemakers.surf.support.ConcurrencyTestHelper;
import jakarta.persistence.EntityManager;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 예약 발행 시간 변경(updateReservationPost)의 동시성/재발행 가드 회귀 테스트.
 *
 * <p>수정 전 문제:
 * <ol>
 *   <li>동시 예약 변경 N건이 같은 RESERVED 예약을 잠금 없이 읽어 각자 cancel() + 새 RESERVED save()
 *       → 한 postId 에 RESERVED 행이 2개 이상 생기고, 이후 게시글 상세조회의 단건 Optional 조회
 *       (findByPostIdAndStatus)가 IncorrectResultSizeDataAccessException 으로 영구 500이 됐다.</li>
 *   <li>이미 발행 완료된 게시글(isReserved=false)에도 예약 변경이 통과되어 재발행/중복 알림이 가능했다.</li>
 * </ol>
 *
 * <p>수정 후 기대 동작: 게시글 행 비관적 락(post 행은 postId 당 항상 존재하는 유일한 직렬화 앵커)으로
 * 동시 예약 변경이 직렬화되어 RESERVED 는 항상 정확히 1행이고, 발행 완료 게시글의 예약 변경은
 * 도메인 예외(ReservationAlreadyPublishedException, 409)로 거부된다.
 *
 * <p>각 스레드가 독립 트랜잭션으로 커밋해야 재현되므로 클래스 트랜잭션을 NOT_SUPPORTED 로 비활성화한다
 * (PostLikeConcurrencyTest 와 동일 패턴). 스케줄러 등록(ReservationScheduleService)은 검증 대상이
 * 아니므로 mock — 인메모리 태스크 미취소 문제는 별도 설계 과제.
 */
@DataJpaTest
@Import({
        ReservationUsecase.class,
        ReservationGetService.class,
        ReservationCreateService.class,
        PostGetService.class,
})
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → 스레드별 독립 커밋
class ReservationUpdateConcurrencyTest {

    private static final int CONCURRENT_UPDATES = 5;

    @Autowired
    private ReservationUsecase reservationUsecase;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private ReservationScheduleService reservationScheduleService;

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

    @BeforeEach
    void setUp() {
        // 클래스 트랜잭션(NOT_SUPPORTED)이 없으므로, 준비 데이터는 별도 트랜잭션으로 명시적 커밋한다.
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
                    .isReserved(true)
                    .build();
            entityManager.persist(post);

            // 최초 예약(RESERVED) 1행 — 예약 변경의 전제 상태
            entityManager.persist(Reservation.of(post.getId(), Instant.now().plusSeconds(3600)));

            this.postId = post.getId();
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
    @DisplayName("동시 예약 변경 5건이 경합해도 RESERVED 예약은 정확히 1행이어야 한다")
    void 동시_예약변경시_RESERVED는_정확히_1행() throws InterruptedException {
        LocalDateTime changedAt = LocalDateTime.now().plusDays(1);

        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_UPDATES,
                () -> reservationUsecase.updateReservationPost(postId, changedAt)
        );

        // 수정 전에는 잠금 없는 조회로 두 요청 모두 같은 RESERVED 를 cancel 하고 각자 새 RESERVED 를 insert 했다.
        assertThat(result.failureCount())
                .as("게시글 행 락으로 직렬화된 예약 변경은 전부 성공해야 한다 (failures=%s)", result.failures())
                .isZero();
        assertThat(countReservedRows())
                .as("RESERVED 가 2행 이상이면 상세조회의 단건 Optional 조회가 IncorrectResultSize 500 이 된다")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 발행 완료된 게시글의 예약 변경은 도메인 예외(409)로 거부된다")
    void 발행_완료_게시글_재예약_거부() {
        // 발행 완료 상태로 전이 (isReserved=false)
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> entityManager.find(Post.class, postId).publish());

        assertThatThrownBy(() -> reservationUsecase.updateReservationPost(postId, LocalDateTime.now().plusDays(1)))
                .as("발행 완료 게시글을 재예약하면 스케줄 도래 시 재발행/중복 알림이 발생한다")
                .isInstanceOf(ReservationAlreadyPublishedException.class);
    }

    /** postId 의 RESERVED 예약 행 수를 새 읽기 트랜잭션에서 조회한다. */
    private Long countReservedRows() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery("select count(r) from Reservation r where r.postId = :pid and r.status = :status", Long.class)
                .setParameter("pid", postId)
                .setParameter("status", ReservationStatus.RESERVED)
                .getSingleResult());
    }
}
