package com.tavemakers.surf.domain.team.service;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.member.query.TrackGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.event.ScoreMemberDismissListener;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;
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
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 관리자 직접 팀 삭제(TeamService.deleteTeam)의 팀 부속 데이터 정리 회귀 테스트.
 *
 * <p>수정 전 문제: 모든 팀은 생성 시 PersonalActivityScore(team_id FK) 1행이 생기는데,
 * 제명 경로(TeamMemberCleanupService.cleanupOnDismiss)와 달리 deleteTeam 은
 * TeamDeletedEvent 를 발행하지 않아 점수/활동기록이 정리되지 않았다
 * — FK 제약이 있으면 팀 삭제가 항상 500, 없으면 고아 행.
 *
 * <p>수정 후 기대 동작: deleteTeam 이 팀 삭제 전에 TeamDeletedEvent 를 발행하고,
 * 동기 리스너(ScoreMemberDismissListener)가 같은 트랜잭션에서 팀 점수를 먼저 벌크 삭제한다
 * (cleanupOnDismiss 와 동일 순서). 픽스처는 MemberDismissTeamCleanupTest 패턴 참고.
 */
@DataJpaTest
@Import({
        TeamService.class,
        ScoreMemberDismissListener.class,
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamDeleteCleanupTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // deleteTeam 경로에서 사용되지 않는 TeamService 의존성
    @MockBean
    private MemberGetService memberGetService;
    @MockBean
    private TrackGetService trackGetService;
    @MockBean
    private PersonalScoreCreateService personalScoreCreateService;

    private Long teamId;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Member leader = persistMember("leader");

            Team team = Team.of(9, TeamType.PROJECT, "삭제대상팀", "관리자 직접 삭제", leader);
            entityManager.persist(team);
            entityManager.persist(PersonalActivityScore.from(team)); // 팀 생성 시 항상 생기는 부속 데이터

            this.teamId = team.getId();
        });
    }

    @Test
    @DisplayName("관리자 직접 팀 삭제 시 TeamDeletedEvent 리스너가 팀 점수를 같은 트랜잭션에서 정리해 팀·팀점수가 모두 삭제된다")
    void 팀_삭제시_팀점수도_함께_정리된다() {
        // 수정 전에는 team_id FK 를 가진 점수 행이 남아 FK 위반(500) 또는 고아 행이 됐다.
        // 트랜잭션 경계는 TeamUsecase.deleteTeam(@Transactional)가 소유하므로(B안),
        // 프로덕션 경계를 재현하기 위해 TransactionTemplate 으로 감싸 호출한다.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThatCode(() -> tx.executeWithoutResult(status -> teamService.deleteTeam(teamId)))
                .doesNotThrowAnyException();

        assertThat(countTeams(teamId))
                .as("팀 row 가 삭제되어야 한다").isZero();
        assertThat(countTeamScores(teamId))
                .as("삭제된 팀의 팀 점수는 TeamDeletedEvent 리스너가 정리해야 한다 (미발행 회귀 감지 지점)")
                .isZero();
    }

    // ===== 검증 헬퍼: 새 읽기 트랜잭션 =====

    private long countTeams(Long teamId) {
        return queryLong("select count(t) from Team t where t.id = :id", teamId);
    }

    private long countTeamScores(Long teamId) {
        return queryLong("select count(s) from PersonalActivityScore s where s.team.id = :id", teamId);
    }

    private long queryLong(String jpql, Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery(jpql, Long.class)
                .setParameter("id", id)
                .getSingleResult());
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
