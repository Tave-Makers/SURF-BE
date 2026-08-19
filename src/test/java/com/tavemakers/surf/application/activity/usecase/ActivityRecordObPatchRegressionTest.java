package com.tavemakers.surf.application.activity.usecase;

import com.tavemakers.surf.application.activity.mapper.ActivityRecordMapper;
import com.tavemakers.surf.application.activity.query.ActivityRecordGetService;
import com.tavemakers.surf.application.score.query.PersonalScoreGetService;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordCreateService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordDeleteService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordPatchService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.util.ScoreCalculator;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.presentation.activity.dto.activityRecord.request.ActivityRecordPatchReqDTO;
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

@ExtendWith(MockitoExtension.class)
class ActivityRecordObPatchRegressionTest {

    private static final ActivityType REWARD_TYPE = ActivityType.ENGAGE_TECH_SEMINAR;
    private static final ActivityType PENALTY_TYPE = ActivityType.NO_SHOW_AFTER_PARTY;
    private static final long MEMBER_ID = 1L;
    private static final long RECORD_ID = 10L;

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
    @Mock
    private ActivityRecordMapper activityRecordMapper;

    private ActivityRecordUsecase usecase;

    @BeforeEach
    void setUp() {
        usecase = new ActivityRecordUsecase(
                activityRecordCreateService,
                activityRecordGetService,
                new ActivityRecordPatchService(),
                activityRecordDeleteService,
                activityRecordMapper,
                personalScoreGetService,
                scoreCalculator,
                logEventEmitter
        );
    }

    @Test
    @DisplayName("OB 회원의 활동기록 유형 수정 시 appliedScore 는 절반 점수를 유지한다")
    void patchActivityRecord_keepsHalfScoreForObMember() {
        Member obMember = Member.builder()
                .name("ob")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.OB)
                .activityStatus(true)
                .build();

        PersonalActivityScore score = PersonalActivityScore.builder()
                .member(obMember)
                .score(BigDecimal.valueOf(50).setScale(1))
                .rewardPrefixSum(BigDecimal.ZERO.setScale(1))
                .penaltyPrefixSum(BigDecimal.ZERO.setScale(1))
                .build();
        score.updateScore(REWARD_TYPE);

        ActivityRecord record = ActivityRecord.builder()
                .memberId(MEMBER_ID)
                .category(REWARD_TYPE.getCategory())
                .activityType(REWARD_TYPE)
                .scoreType(REWARD_TYPE.getScoreType())
                .activityDate(LocalDate.now())
                .appliedScore(BigDecimal.valueOf(5).setScale(1))
                .prefixSum(score.getScore())
                .isDeleted(false)
                .build();

        given(activityRecordGetService.findByIdForUpdate(RECORD_ID)).willReturn(record);
        given(personalScoreGetService.getPersonalScoreForUpdate(MEMBER_ID)).willReturn(score);

        usecase.patchActivityRecord(RECORD_ID, new ActivityRecordPatchReqDTO(PENALTY_TYPE, null));

        assertThat(record.getAppliedScore()).isEqualByComparingTo("-5.0");
        assertThat(score.getScore()).isEqualByComparingTo("45.0");
        assertThat(score.getRewardPrefixSum()).isEqualByComparingTo("0.0");
        assertThat(score.getPenaltyPrefixSum()).isEqualByComparingTo("-5.0");
    }
}
