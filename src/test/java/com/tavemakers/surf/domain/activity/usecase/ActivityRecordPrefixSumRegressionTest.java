package com.tavemakers.surf.domain.activity.usecase;

import com.tavemakers.surf.domain.activity.dto.activityRecord.request.ActivityRecordPatchReqDTO;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordCreateService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordDeleteService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordGetService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordPatchService;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import com.tavemakers.surf.domain.score.utils.ScoreCalculator;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 활동기록 수정/삭제 시 상·벌점 누적합(rewardPrefixSum/penaltyPrefixSum) 정합 회귀 테스트.
 *
 * <p>부여 경로(updateScore(ActivityType))는 score 와 prefixSum 을 함께 갱신하지만,
 * 수정/삭제 경로가 score 만 보정하고 prefixSum 을 되돌리지 않으면 관리자 랭킹의
 * 상점/벌점 총합이 영구 오염된다. 이 테스트는 부여 → 삭제/유형변경 후
 * score 와 두 prefixSum 이 모두 정합인지 검증한다.
 *
 * <p>H2 의 ActivityRecord DDL(TINYINT(1)) 파싱 한계로 @DataJpaTest 저장이 불가하여
 * (MemberDismissRollbackTest 주석 참고), ActivityRecord 는 인메모리 엔티티로 구성하고
 * repository 계층 서비스는 mock 으로 대체한 usecase 레벨 테스트로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ActivityRecordPrefixSumRegressionTest {

    private static final ActivityType REWARD_TYPE = ActivityType.ENGAGE_TECH_SEMINAR; // +10, REWARD
    private static final ActivityType PENALTY_TYPE = ActivityType.NO_SHOW_AFTER_PARTY; // -10, PENALTY
    private static final long MEMBER_ID = System.nanoTime();
    private static final long RECORD_ID = System.nanoTime();

    @Mock
    private ActivityRecordCreateService activityRecordCreateService;
    @Mock
    private ActivityRecordGetService activityRecordGetService;
    @Mock
    private ActivityRecordDeleteService activityRecordDeleteService;
    @Mock
    private PersonalScoreGetService personalScoreGetService;
    @Mock
    private ScoreCalculator scoreCalculator;
    @Mock
    private LogEventEmitter logEventEmitter;

    private ActivityRecordUsecase usecase;

    private PersonalActivityScore score;
    private ActivityRecord record;

    @BeforeEach
    void setUp() {
        // patch 서비스는 의존성 없는 위임 계층이므로 실제 인스턴스를 사용해 엔티티 상태 전이까지 검증한다.
        usecase = new ActivityRecordUsecase(
                activityRecordCreateService,
                activityRecordGetService,
                new ActivityRecordPatchService(),
                activityRecordDeleteService,
                personalScoreGetService,
                scoreCalculator,
                logEventEmitter
        );

        // 초기 점수 100 에 상점(+10) 부여 → score=110, rewardPrefixSum=10
        score = PersonalActivityScore.builder()
                .score(BigDecimal.valueOf(100))
                .rewardPrefixSum(BigDecimal.ZERO)
                .penaltyPrefixSum(BigDecimal.ZERO)
                .build();
        score.updateScore(REWARD_TYPE);

        record = ActivityRecord.builder()
                .memberId(MEMBER_ID)
                .category(REWARD_TYPE.getCategory())
                .activityType(REWARD_TYPE)
                .scoreType(REWARD_TYPE.getScoreType())
                .activityDate(LocalDate.now())
                .appliedScore(BigDecimal.valueOf(REWARD_TYPE.getDelta()))
                .prefixSum(score.getScore())
                .isDeleted(false)
                .build();

        given(activityRecordGetService.findById(RECORD_ID)).willReturn(record);
        given(personalScoreGetService.getPersonalScoreForUpdate(MEMBER_ID)).willReturn(score);
    }

    @Test
    @DisplayName("상점 기록 삭제 시 score 와 rewardPrefixSum 이 모두 부여 전으로 원복된다")
    void 상점기록_삭제시_score와_rewardPrefixSum_모두_원복() {
        usecase.deleteActivityRecord(RECORD_ID);

        assertThat(score.getScore())
                .as("score 는 부여 전(100)으로 원복").isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(score.getRewardPrefixSum())
                .as("rewardPrefixSum 도 부여 전(0)으로 원복").isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(score.getPenaltyPrefixSum())
                .as("penaltyPrefixSum 은 변화 없음").isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("상점→벌점 유형 변경 시 rewardPrefixSum 감소, penaltyPrefixSum 증가, score 정합")
    void 상점에서_벌점으로_유형변경시_양쪽_prefixSum_정합() {
        usecase.patchActivityRecord(RECORD_ID, new ActivityRecordPatchReqDTO(PENALTY_TYPE, null));

        assertThat(score.getScore())
                .as("score = 100(초기) - 10(벌점)").isEqualByComparingTo(BigDecimal.valueOf(90));
        assertThat(score.getRewardPrefixSum())
                .as("구 상점(+10) 제거로 0").isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(score.getPenaltyPrefixSum())
                .as("신 벌점(-10) 반영").isEqualByComparingTo(BigDecimal.valueOf(-10));
    }
}
