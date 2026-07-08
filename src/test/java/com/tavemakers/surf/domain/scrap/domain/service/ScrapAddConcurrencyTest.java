package com.tavemakers.surf.domain.scrap.domain.service;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.application.query.MemberGetService;
import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.post.domain.repository.PostRepository;
import com.tavemakers.surf.domain.post.application.query.PostLikeGetService;
import com.tavemakers.surf.domain.post.application.query.PostGetService;
import com.tavemakers.surf.domain.scrap.domain.exception.ScrapAlreadyExistsException;
import com.tavemakers.surf.domain.scrap.domain.repository.ScrapRepository;
import com.tavemakers.surf.global.logging.LogEventEmitter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;

/**
 * ScrapService.addScrap 멱등성/동시성 회귀 테스트.
 *
 * <p>수정 전 문제: 동시 중복 스크랩 시 save 후 커밋 시점에 unique 위반이 터지면 트랜잭션이
 * rollback-only가 되어, 예외를 catch 후 return 해도 커밋 단계에서 UnexpectedRollbackException(500)이 됐다.
 *
 * <p>수정 후 기대 동작:
 * <ol>
 *   <li>순차 재클릭(이미 스크랩됨)은 exists 체크로 조기 종료 → 예외 없이 멱등, row 1개.</li>
 *   <li>동시 race의 패자는 saveAndFlush의 DataIntegrityViolationException을 잡아
 *       도메인 예외(ScrapAlreadyExistsException, 409)로 정직하게 전파. row는 정확히 1개.</li>
 * </ol>
 *
 * <p>각 스레드가 독립 트랜잭션으로 커밋해야 race가 재현되므로 클래스 트랜잭션을 NOT_SUPPORTED로
 * 비활성화하고, ScrapService를 스프링 프록시 빈으로 등록해 addScrap의 @Transactional이 스레드별로 동작하게 한다.
 * H2는 MODE=MySQL 이라 unique 제약 위반 시 MySQL과 동일하게 DataIntegrityViolationException으로 매핑된다.
 */
@DataJpaTest
@Import(ScrapService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → 스레드별 독립 커밋
class ScrapAddConcurrencyTest {

    private static final int CONCURRENT_THREADS = 5;

    @Autowired
    private ScrapService scrapService;

    @Autowired
    private ScrapRepository scrapRepository;

    // PostGetService는 mock이지만, scrapCount 검증을 위해 실제 PostRepository로 위임한다.
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // addScrap의 부수효과(로그 AOP)와 타 도메인 조회는 mock. 검증 대상은 Scrap row·scrapCount·예외 타입뿐이다.
    @MockBean
    private MemberGetService memberGetService;
    @MockBean
    private PostGetService postGetService;
    @MockBean
    private PostLikeGetService postLikeGetService;
    @MockBean
    private LogEventEmitter logEventEmitter;

    private Long memberId;
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

            // 클래스 트랜잭션이 없어 테스트 간 데이터가 남으므로, 이메일은 실행마다 고유하게 만든다 (unique 충돌 방지)
            Member author = persistMember("author" + System.nanoTime() + "@test.com");

            Post post = Post.builder()
                    .title("제목").content("내용")
                    .board(board).boardName(board.getName())
                    .category(category).categoryName(category.getName())
                    .member(author)
                    .build();
            entityManager.persist(post);

            this.memberId = author.getId();
            this.postId = post.getId();
        });

        // addScrap 내부의 타 도메인 조회는 mock 처리. 각 스레드 트랜잭션에서 필요한 엔티티를 다시 로드해 반환한다.
        given(memberGetService.getMember(anyLong()))
                .willAnswer(inv -> loadInReadTx(Member.class, inv.getArgument(0)));
        given(postGetService.getPost(anyLong()))
                .willAnswer(inv -> loadInReadTx(Post.class, inv.getArgument(0)));

        // scrapCount 원자적 증가(UPDATE)는 실제 PostRepository로 위임해 실제 DB 상태를 반영한다.
        // @Modifying UPDATE는 활성 트랜잭션을 요구하는데, addScrap이 연 트랜잭션 안에서 호출되므로 그대로 참여한다.
        willAnswer(inv -> {
            postRepository.increaseScrapCount(inv.getArgument(0));
            return null;
        }).given(postGetService).increaseScrapCount(anyLong());
    }

    /** mock 응답용: 새 읽기 트랜잭션에서 엔티티를 로드해 detached로 반환한다. */
    private <T> T loadInReadTx(Class<T> type, Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(type, id));
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
    @DisplayName("같은 회원이 같은 게시글을 순차로 2번 스크랩하면 예외 없이 멱등 처리되고 row와 scrapCount는 1이다")
    void 순차_재클릭은_exists체크로_멱등종료_row와_scrapCount는_1() {
        // addScrap의 @Transactional 프록시가 트랜잭션 시작·커밋을 담당하므로 직접 호출한다.
        // 1차: 신규 스크랩 (insert + scrapCount 증가)
        scrapService.addScrap(memberId, postId);
        // 2차: 이미 스크랩됨 → exists 체크에서 조기 종료. 예외가 나면 회귀.
        Throwable second = catchThrowable(() -> scrapService.addScrap(memberId, postId));

        assertThat(second)
                .as("순차 재클릭은 exists 체크로 예외 없이 멱등 종료되어야 한다")
                .isNull();
        assertThat(countScrapRowsInReadTx())
                .as("중복 스크랩이 생기지 않고 row는 1개여야 한다")
                .isEqualTo(1L);
        assertThat(loadScrapCountInReadTx())
                .as("2차 호출은 조기 종료되어 scrapCount는 1만 증가해야 한다")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 회원·게시글에 5스레드가 동시 스크랩하면 row는 1개이고 실패는 전부 ScrapAlreadyExistsException이다")
    void 동시_중복_스크랩_race시_row는_1개이고_실패는_전부_도메인예외() throws InterruptedException {
        // addScrap의 @Transactional 프록시가 스레드마다 트랜잭션 시작·커밋을 담당하므로 직접 호출한다.
        // (테스트가 트랜잭션을 밖에서 감싸면 커밋 시점 unique 위반이 addScrap의 try-catch를 우회해
        //  DataIntegrityViolationException 이 테스트로 새어 결과가 flaky 해진다.)
        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_THREADS,
                () -> scrapService.addScrap(memberId, postId)
        );

        // 불변식 ① 실패한 스레드의 예외는 전부 도메인 예외(409)여야 한다.
        //          DataIntegrityViolationException / UnexpectedRollbackException 이 새어나오면 회귀로 판정한다.
        //          (allSatisfy 는 빈 목록도 통과 — 완전 직렬화되어 나머지가 exists 로 멱등 종료되는 경우도 정상)
        assertThat(result.failures())
                .as("동시 race 의 패자는 전부 ScrapAlreadyExistsException 이어야 한다 "
                        + "(DataIntegrityViolationException/UnexpectedRollbackException 이 새면 회귀)")
                .allSatisfy(t -> assertThat(t).isInstanceOf(ScrapAlreadyExistsException.class));

        // 불변식 ② 최종 Scrap row 는 unique 제약으로 정확히 1개 (중복 insert 가 커밋되지 않음).
        assertThat(countScrapRowsInReadTx())
                .as("unique 제약으로 Scrap row 는 정확히 1개여야 한다")
                .isEqualTo(1L);

        // 불변식 ③ scrapCount 는 실제 커밋된 스크랩 1건에 대해서만 증가해 1이어야 한다.
        assertThat(loadScrapCountInReadTx())
                .as("커밋에 성공한 스크랩 1건만 scrapCount 를 증가시켜 1이어야 한다")
                .isEqualTo(1L);
    }

    /** Scrap row 수를 새 읽기 트랜잭션에서 조회한다. */
    private Long countScrapRowsInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery("select count(s) from Scrap s where s.member.id = :mid and s.post.id = :pid", Long.class)
                .setParameter("mid", memberId)
                .setParameter("pid", postId)
                .getSingleResult());
    }

    /** Post.scrapCount를 새 읽기 트랜잭션에서 조회한다. */
    private Long loadScrapCountInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(Post.class, postId).getScrapCount());
    }
}
