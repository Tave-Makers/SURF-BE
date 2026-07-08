package com.tavemakers.surf.domain.activity.application.usecase;

import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.request.ActivityRecordPatchReqDTO;
import com.tavemakers.surf.domain.activity.domain.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.domain.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.domain.exception.ActivityRecordAlreadyDeletedException;
import com.tavemakers.surf.domain.activity.domain.service.activityRecord.ActivityRecordCreateService;
import com.tavemakers.surf.domain.activity.domain.service.activityRecord.ActivityRecordDeleteService;
import com.tavemakers.surf.domain.activity.application.query.ActivityRecordGetService;
import com.tavemakers.surf.domain.activity.domain.service.activityRecord.ActivityRecordPatchService;
import com.tavemakers.surf.domain.score.domain.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.application.query.PersonalScoreGetService;
import com.tavemakers.surf.domain.score.domain.util.ScoreCalculator;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 활동기록 동시 삭제/수정 double-apply 회귀 테스트.
 *
 * <p>동시 삭제 2건에서 T1이 record 락을 잡고 점수 되돌림 + softDelete 후 커밋하면,
 * T2는 락 없는 findById로 읽은 stale record(isDeleted=false)를 통과시켜 점수를
 * 한 번 더 되돌린다(이중 차감). 수정 경로도 구(舊) 값 되돌림이 중복될 수 있다.
 *
 * <p>이 테스트는 "T2 관점"을 단일 스레드로 결정적으로 재현한다: 행 잠금 조회
 * (findByIdForUpdate)는 T1 커밋 후의 최신 상태(isDeleted=true)를 반환하고, 락 없는
 * findById는 stale 상태(isDeleted=false)를 반환하도록 구성한다. 수정 후 usecase는
 * 잠금 조회를 사용하므로 AlreadyDeleted 예외로 중단되고 점수는 건드리지 않는다.
 * (수정 전 usecase는 findById의 stale 상태로 진행해 점수를 이중 보정 → 테스트 실패)
 *
 * <p>H2 의 ActivityRecord DDL(TINYINT(1)) 파싱 한계로 @DataJpaTest 저장이 불가하여
 * ActivityRecordPrefixSumRegressionTest와 동일하게 mock 기반 usecase 테스트로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ActivityRecordDoubleApplyRegressionTest {

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
    private ActivityRecord staleRecord;
    private ActivityRecord freshDeletedRecord;

    @BeforeEach
    void setUp() {
        usecase = new ActivityRecordUsecase(
                activityRecordCreateService,
                activityRecordGetService,
                new ActivityRecordPatchService(),
                activityRecordDeleteService,
                personalScoreGetService,
                scoreCalculator,
                logEventEmitter
        );

        // T1이 이미 삭제를 완료한 상태: 점수는 부여 전(100)으로 원복 완료
        score = PersonalActivityScore.builder()
                .score(BigDecimal.valueOf(100))
                .rewardPrefixSum(BigDecimal.ZERO)
                .penaltyPrefixSum(BigDecimal.ZERO)
                .build();

        // T2의 영속성 컨텍스트가 보는 stale record — T1 커밋 반영 전 (isDeleted=false)
        staleRecord = buildRecord(false);
        // 행 잠금 조회가 T1 커밋 대기 후 읽는 최신 record (isDeleted=true)
        freshDeletedRecord = buildRecord(true);

        // 수정 전 코드(findById 사용)가 stale 상태로 진행해 이중 보정함을 재현하기 위한 스텁.
        // 수정 후 코드는 findByIdForUpdate만 사용하므로 lenient 처리.
        lenient().when(activityRecordGetService.findById(RECORD_ID)).thenReturn(staleRecord);
        lenient().when(personalScoreGetService.getPersonalScoreForUpdate(MEMBER_ID)).thenReturn(score);
        given(activityRecordGetService.findByIdForUpdate(RECORD_ID)).willReturn(freshDeletedRecord);
    }

    private ActivityRecord buildRecord(boolean isDeleted) {
        return ActivityRecord.builder()
                .memberId(MEMBER_ID)
                .category(REWARD_TYPE.getCategory())
                .activityType(REWARD_TYPE)
                .scoreType(REWARD_TYPE.getScoreType())
                .activityDate(LocalDate.now())
                .appliedScore(BigDecimal.valueOf(REWARD_TYPE.getDelta()))
                .prefixSum(BigDecimal.valueOf(110))
                .isDeleted(isDeleted)
                .build();
    }

    @Test
    @DisplayName("동시 삭제 T2는 락 후 최신 isDeleted=true를 보고 예외로 중단, 점수 이중 차감 없음")
    void 동시삭제_T2는_이중차감_없이_예외로_중단() {
        assertThatThrownBy(() -> usecase.deleteActivityRecord(RECORD_ID))
                .isInstanceOf(ActivityRecordAlreadyDeletedException.class);

        assertThat(score.getScore())
                .as("점수는 T1의 원복 결과(100) 그대로 — 이중 차감 없음")
                .isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(score.getRewardPrefixSum())
                .as("rewardPrefixSum 도 변화 없음").isEqualByComparingTo(BigDecimal.ZERO);
        verify(activityRecordDeleteService, never()).softDelete(any());
    }

    @Test
    @DisplayName("동시 수정 T2는 락 후 최신 isDeleted=true를 보고 예외로 중단, 구 값 되돌림 중복 없음")
    void 동시수정_T2는_구값_되돌림_중복_없이_예외로_중단() {
        assertThatThrownBy(() ->
                usecase.patchActivityRecord(RECORD_ID, new ActivityRecordPatchReqDTO(PENALTY_TYPE, null)))
                .isInstanceOf(ActivityRecordAlreadyDeletedException.class);

        assertThat(score.getScore())
                .as("점수는 T1의 결과(100) 그대로 — 구 값 되돌림 중복 없음")
                .isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(score.getPenaltyPrefixSum())
                .as("penaltyPrefixSum 변화 없음").isEqualByComparingTo(BigDecimal.ZERO);
    }
}
