package com.tavemakers.surf.application.activity.usecase;

import com.tavemakers.surf.presentation.activity.dto.activityRecord.request.ActivityRecordReqDTO;
import com.tavemakers.surf.presentation.activity.dto.activityRecord.request.ActivityRecordReqDTOV2;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.application.activity.mapper.ActivityRecordMapper;
import com.tavemakers.surf.application.activity.query.ActivityRecordGetService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordCreateService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordDeleteService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordPatchService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.exception.PersonalScoreNotFoundException;
import com.tavemakers.surf.application.score.query.PersonalScoreGetService;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import com.tavemakers.surf.domain.score.util.ScoreCalculator;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 점수 행(PersonalActivityScore) 미존재 회원 점수 부여 silent no-op 회귀 테스트.
 *
 * <p>applyActivityRecord/createActivityRecordList는 기존 점수 행만 조회해 루프를 돌기 때문에,
 * 행이 없는 회원은 조회 결과에서 빠져 기록 생성 없이 그대로 성공(201)을 반환했다.
 * 관리자는 점수를 부여했다고 믿지만 실제로는 아무것도 반영되지 않는 무결성 문제다.
 *
 * <p>수정 후에는 조회된 점수 행 수가 요청 대상 수와 다르면 PersonalScoreNotFoundException으로
 * 명확히 실패해야 한다. (수정 전에는 예외 없이 통과 → 이 테스트 실패)
 *
 * <p>H2의 ActivityRecord DDL(TINYINT(1)) 파싱 한계로 @DataJpaTest 저장이 불가하여
 * 기존 회귀 테스트와 동일하게 mock 리포지토리 + 실제 PersonalScoreGetService 조합으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ActivityRecordMissingScoreRegressionTest {

    private static final ActivityType REWARD_TYPE = ActivityType.ENGAGE_TECH_SEMINAR; // +10, REWARD
    private static final long MEMBER_A_ID = 101L;
    private static final long MEMBER_B_ID = 102L;
    private static final long TEAM_ID = 201L;

    @Mock
    private ActivityRecordCreateService activityRecordCreateService;
    @Mock
    private ActivityRecordGetService activityRecordGetService;
    @Mock
    private ActivityRecordDeleteService activityRecordDeleteService;
    @Mock
    private ScoreCalculator scoreCalculator;
    @Mock
    private LogEventEmitter logEventEmitter;
    @Mock
    private ActivityRecordMapper activityRecordMapper;
    @Mock
    private PersonalActivityScoreRepository personalScoreRepository;

    private ActivityRecordUsecase usecase;

    @BeforeEach
    void setUp() {
        // 버그가 조회 계층(PersonalScoreGetService)과 usecase 사이에 걸쳐 있으므로 GetService는 실제 객체를 사용
        usecase = new ActivityRecordUsecase(
                activityRecordCreateService,
                activityRecordGetService,
                new ActivityRecordPatchService(),
                activityRecordDeleteService,
                activityRecordMapper,
                new PersonalScoreGetService(personalScoreRepository),
                scoreCalculator,
                logEventEmitter
        );
    }

    private PersonalActivityScore scoreOf(Member member) {
        return PersonalActivityScore.builder()
                .member(member)
                .score(BigDecimal.valueOf(100))
                .rewardPrefixSum(BigDecimal.ZERO)
                .penaltyPrefixSum(BigDecimal.ZERO)
                .build();
    }

    private Member approvedMember() {
        return Member.builder()
                .provider(Provider.KAKAO)
                .providerId("provider-a")
                .name("회원A")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
    }

    @Test
    @DisplayName("V2 개인 부여 — 점수 행 없는 회원이면 silent no-op 대신 예외로 실패한다")
    void V2_개인부여_점수행_없으면_예외() {
        given(personalScoreRepository.findAllByMemberIdInForUpdate(List.of(MEMBER_A_ID)))
                .willReturn(List.of()); // 점수 행 미존재

        ActivityRecordReqDTOV2 dto = new ActivityRecordReqDTOV2(
                List.of(MEMBER_A_ID), null, null, null, REWARD_TYPE, LocalDate.now());

        assertThatThrownBy(() -> usecase.applyActivityRecord(dto))
                .isInstanceOf(PersonalScoreNotFoundException.class);

        verify(activityRecordCreateService, never()).saveActivityRecordList(any());
    }

    @Test
    @DisplayName("V2 개인 부여 — 일부 회원만 점수 행이 없어도 부분 반영 없이 예외로 실패한다")
    void V2_개인부여_일부_점수행_없으면_부분반영_없이_예외() {
        // A만 점수 행 존재, B는 미존재 → 요청 2명 중 1명만 조회됨
        given(personalScoreRepository.findAllByMemberIdInForUpdate(List.of(MEMBER_A_ID, MEMBER_B_ID)))
                .willReturn(List.of(scoreOf(approvedMember())));

        ActivityRecordReqDTOV2 dto = new ActivityRecordReqDTOV2(
                List.of(MEMBER_A_ID, MEMBER_B_ID), null, null, null, REWARD_TYPE, LocalDate.now());

        assertThatThrownBy(() -> usecase.applyActivityRecord(dto))
                .isInstanceOf(PersonalScoreNotFoundException.class);

        verify(activityRecordCreateService, never()).saveActivityRecordList(any());
    }

    @Test
    @DisplayName("V2 팀 부여 — 점수 행 없는 팀이면 silent no-op 대신 예외로 실패한다")
    void V2_팀부여_점수행_없으면_예외() {
        given(personalScoreRepository.findAllByTeamIdInForUpdate(List.of(TEAM_ID)))
                .willReturn(List.of()); // 팀 점수 행 미존재

        ActivityRecordReqDTOV2 dto = new ActivityRecordReqDTOV2(
                null, List.of(TEAM_ID), null, null, REWARD_TYPE, LocalDate.now());

        assertThatThrownBy(() -> usecase.applyActivityRecord(dto))
                .isInstanceOf(PersonalScoreNotFoundException.class);

        verify(activityRecordCreateService, never()).saveActivityRecordList(any());
    }

    @Test
    @DisplayName("V1 다수 부여 — 점수 행 없는 회원이 섞여 있으면 예외로 실패한다")
    void V1_다수부여_점수행_없으면_예외() {
        given(personalScoreRepository.findAllByMemberIdInForUpdate(List.of(MEMBER_A_ID, MEMBER_B_ID)))
                .willReturn(List.of(scoreOf(approvedMember())));

        ActivityRecordReqDTO dto = new ActivityRecordReqDTO(
                List.of(MEMBER_A_ID, MEMBER_B_ID), null, REWARD_TYPE, LocalDate.now());

        assertThatThrownBy(() -> usecase.createActivityRecordList(dto))
                .isInstanceOf(PersonalScoreNotFoundException.class);

        verify(activityRecordCreateService, never()).saveActivityRecordList(any());
    }
}
