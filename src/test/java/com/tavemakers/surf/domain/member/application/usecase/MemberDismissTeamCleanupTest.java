package com.tavemakers.surf.domain.member.application.usecase;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;
import com.tavemakers.surf.domain.comment.domain.service.CommentDeleteService;
import com.tavemakers.surf.domain.comment.domain.service.CommentLikeService;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.domain.repository.MemberRepository;
import com.tavemakers.surf.domain.member.domain.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.domain.service.MemberWithdrawService;
import com.tavemakers.surf.domain.post.domain.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.application.usecase.PostDeleteUsecase;
import com.tavemakers.surf.domain.score.domain.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.domain.event.ScoreMemberDismissListener;
import com.tavemakers.surf.domain.scrap.domain.service.ScrapService;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.team.service.TeamMemberCleanupService;
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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 제명 시 팀 정리(리더 위임 + 팀 삭제 + 팀 부속 데이터)의 회귀 테스트 (리뷰 B2).
 *
 * <p>재현 시나리오: 제명 대상이 (A) 혼자 리더인 팀과 (B) 리더 위임이 필요한 팀에 동시 소속.
 * 팀 A 순회에서 TeamDeletedEvent 가 발행되고 동기 리스너(ScoreMemberDismissListener)가
 * deleteByTeamId 벌크를 실행하는데, 이 벌크가 clearAutomatically 로 영속성 컨텍스트를 클리어하면
 * 아직 순회하지 않은 팀 B 가 detach 되어 changeLeader 의 dirty checking 이 유실되고, 최종
 * member 삭제에서 team.leader_member_id FK 위반으로 제명 전체가 실패한다.
 *
 * <p>팀 A 를 먼저 persist 해 낮은 PK 를 부여한다 — findAllByMemberIdForDismissal 에 ORDER BY 가
 * 없어 사실상 PK 순으로 순회되므로, "팀 삭제(이벤트) → 리더 위임" 순서가 재현된다.
 *
 * <p>activity 리스너는 H2 의 ActivityRecord DDL(TINYINT(1)) 한계로 제외
 * (MemberDismissCompletenessTest 참고) — Score 리스너만으로 B2 가 재현된다.
 */
@DataJpaTest
@Import({
        MemberDismissUsecase.class,
        TeamMemberCleanupService.class,
        ScoreMemberDismissListener.class,
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberDismissTeamCleanupTest {

    @Autowired
    private MemberDismissUsecase memberDismissUsecase;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MemberRepository memberRepository;

    @MockBean
    private MemberBlacklistCreateService memberBlacklistCreateService;
    @MockBean
    private MemberWithdrawService memberWithdrawService;
    @MockBean
    private PostDeleteUsecase postDeleteUsecase;
    @MockBean
    private PostLikeService postLikeService;
    @MockBean
    private ScrapService scrapService;
    @MockBean
    private CommentLikeService commentLikeService;
    @MockBean
    private CommentDeleteService commentDeleteService;

    private Long victimId;
    private Long otherId;
    private Long soloTeamId;
    private Long delegateTeamId;

    @BeforeEach
    void setUp() {
        given(postDeleteUsecase.deleteAllOwnedBy(anyLong())).willReturn(Collections.emptySet());

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Member victim = persistMember("victim");
            Member other = persistMember("other");

            // 팀 A: victim 혼자 리더 → 제명 시 팀 삭제 + TeamDeletedEvent 발행 경로 (먼저 persist → 낮은 PK)
            Team soloTeam = Team.of(9, TeamType.PROJECT, "솔로팀", "victim 혼자 리더", victim);
            entityManager.persist(soloTeam);
            entityManager.persist(PersonalActivityScore.from(soloTeam)); // 팀 부속 데이터 (리스너 삭제 대상)

            // 팀 B: victim 리더 + other 팀원 → 제명 시 리더 위임 경로
            Team delegateTeam = Team.of(9, TeamType.STUDY, "위임팀", "리더 위임 필요", victim);
            delegateTeam.addMember(other);
            entityManager.persist(delegateTeam);

            this.victimId = victim.getId();
            this.otherId = other.getId();
            this.soloTeamId = soloTeam.getId();
            this.delegateTeamId = delegateTeam.getId();
        });
    }

    @Test
    @DisplayName("솔로 리더 팀(삭제)과 위임 필요 팀에 동시 소속된 회원을 제명하면, 팀 삭제 리스너의 벌크 delete 가 위임 팀의 리더 변경을 유실시키지 않는다")
    void 팀_삭제_이벤트가_리더_위임을_유실시키지_않는다() {
        Member victim = loadMember(victimId);

        // B2 회귀 시: TeamDeletedEvent 리스너의 clearAutomatically 가 위임 팀을 detach →
        // changeLeader 유실 → member 삭제에서 FK 위반(DataIntegrityViolationException)
        assertThatCode(() -> memberDismissUsecase.dismiss(victim, 999L))
                .doesNotThrowAnyException();

        assertThat(memberRepository.findById(victimId))
                .as("member row 가 삭제되어야 한다").isEmpty();

        assertThat(countTeams(soloTeamId))
                .as("솔로 리더 팀은 삭제되어야 한다").isZero();
        assertThat(countTeamScores(soloTeamId))
                .as("삭제된 팀의 팀 점수는 리스너가 정리해야 한다").isZero();

        assertThat(countTeams(delegateTeamId))
                .as("위임 팀은 남아야 한다").isEqualTo(1);
        assertThat(currentLeaderId(delegateTeamId))
                .as("위임 팀의 리더가 other 로 위임되어 DB 에 반영되어야 한다 (B2 회귀 감지 지점)")
                .isEqualTo(otherId);
        assertThat(countTeamMembers(delegateTeamId, victimId))
                .as("위임 팀에서 victim 소속이 제거되어야 한다").isZero();
    }

    // ===== 검증 헬퍼: 새 읽기 트랜잭션 =====

    private long countTeams(Long teamId) {
        return queryLong("select count(t) from Team t where t.id = :id", teamId);
    }

    private long countTeamScores(Long teamId) {
        return queryLong("select count(s) from PersonalActivityScore s where s.team.id = :id", teamId);
    }

    private Long currentLeaderId(Long teamId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery("select t.leader.id from Team t where t.id = :id", Long.class)
                .setParameter("id", teamId)
                .getSingleResult());
    }

    private long countTeamMembers(Long teamId, Long memberId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery("select count(tm) from TeamMember tm where tm.team.id = :teamId and tm.member.id = :memberId", Long.class)
                .setParameter("teamId", teamId)
                .setParameter("memberId", memberId)
                .getSingleResult());
    }

    private long queryLong(String jpql, Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery(jpql, Long.class)
                .setParameter("id", id)
                .getSingleResult());
    }

    private Member loadMember(Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(Member.class, id));
    }

    private Member persistMember(String prefix) {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(System.nanoTime()))
                .kakaoId(System.nanoTime())
                .name("회원")
                .email(prefix + System.nanoTime() + "@test.com")
                .phoneNumber(String.valueOf(System.nanoTime()))
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        entityManager.persist(member);
        return member;
    }
}
