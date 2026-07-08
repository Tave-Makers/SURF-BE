package com.tavemakers.surf.domain.activity.application.usecase;

import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.request.ActivityRecordPatchReqDTO;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.request.ActivityRecordReqDTO;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.request.ActivityRecordReqDTOV2;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.response.ActivityCategoryDetailResDTO;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.response.ActivityCategoryResDTO;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.response.ActivityRecordResDTO;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.response.ActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.response.AdminActivityRecordResDTO;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.response.AdminActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.domain.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.domain.entity.enums.ActivityCategory;
import com.tavemakers.surf.domain.activity.domain.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.domain.entity.enums.ScoreType;
import com.tavemakers.surf.domain.activity.domain.exception.ActivityRecordAlreadyDeletedException;
import com.tavemakers.surf.domain.activity.domain.service.activityRecord.ActivityRecordDeleteService;
import com.tavemakers.surf.domain.activity.application.query.ActivityRecordGetService;
import com.tavemakers.surf.domain.activity.domain.service.activityRecord.ActivityRecordCreateService;
import com.tavemakers.surf.domain.activity.domain.service.activityRecord.ActivityRecordPatchService;
import com.tavemakers.surf.domain.score.domain.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.application.query.PersonalScoreGetService;
import com.tavemakers.surf.domain.score.domain.util.ScoreCalculator;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityRecordUsecase {

    private final ActivityRecordCreateService activityRecordCreateService;
    private final ActivityRecordGetService activityRecordGetService;
    private final ActivityRecordPatchService activityRecordPatchService;
    private final ActivityRecordDeleteService activityRecordDeleteService;
    private final PersonalScoreGetService personalScoreGetService;
    private final ScoreCalculator scoreCalculator;
    private final LogEventEmitter logEventEmitter;

    /** 다수 회원의 활동기록 생성 및 점수 반영 */
    @Transactional
    public void createActivityRecordList(ActivityRecordReqDTO dto) {
        // 다수의 활동 점수 -> 감점 + 가점 -> 누적합과 함께 활동기록 생성 (행 잠금으로 동시 갱신 직렬화)
        List<PersonalActivityScore> scoreList = personalScoreGetService.getPersonalScoreListByIdsForUpdate(dto.memberIdList());
        List<ActivityRecord> recordList = scoreList.stream()
                .map(personalScore -> {
                    BigDecimal prefixSum = personalScore.updateScore(dto.activityName());
                    return ActivityRecord.of(personalScore.getMember().getId(), dto, prefixSum);
                        }
                ).toList();

        activityRecordCreateService.saveActivityRecordList(recordList);
    }

    /** 활동기록 생성 및 점수 반영 */
    @Transactional
    public void applyActivityRecord(ActivityRecordReqDTOV2 dto) {

        try {

            ActivityType activityType = dto.activityName();

            if (dto.isTeam()) {

                List<PersonalActivityScore> teamScoreList =
                        personalScoreGetService.getTeamScoreListByIdsForUpdate(dto.teamIdList());

                List<ActivityRecord> recordList = teamScoreList.stream()
                        .map(teamScore -> {
                                    BigDecimal prefixSum =
                                            teamScore.updateScore(activityType);

                                    return ActivityRecord.ofTeam(
                                            teamScore.getTeam().getId(),
                                            dto,
                                            prefixSum
                                    );
                                }
                        ).toList();

                activityRecordCreateService.saveActivityRecordList(recordList);

                logEventEmitter.emit("activity.record.create", Map.of(
                        "member_id_list_count", dto.teamIdList().size(),
                        "activity_name", activityType.name(),
                        "activity_date", dto.activityDate()
                ));

                return;
            }

            List<PersonalActivityScore> scoreList =
                    personalScoreGetService.getPersonalScoreListByIdsForUpdate(dto.memberIdList());

            List<ActivityRecord> recordList = scoreList.stream()
                    .map(personalScore -> {
                                BigDecimal prefixSum =
                                        personalScore.updateScore(activityType);

                                return ActivityRecord.ofPersonal(
                                        personalScore.getMember().getId(),
                                        dto,
                                        prefixSum
                                );
                            }
                    ).toList();

            activityRecordCreateService.saveActivityRecordList(recordList);

            logEventEmitter.emit("activity.record.create", Map.of(
                    "member_id_list_count", dto.memberIdList().size(),
                    "activity_name", activityType.name(),
                    "activity_date", dto.activityDate()
            ));

        } catch (Exception e) {

            logEventEmitter.emitError(
                    "activity.record.create.failed",
                    Map.of(
                            "error_code", 500,
                            "error_msg", e.getClass().getSimpleName()
                    ),
                    "활동 기록 생성 실패"
            );

            throw e;
        }
    }

    /** 회원의 활동기록 목록 페이징 조회 */
    @LogEvent(value = "activity.records.list", message = "활동 기록 목록 조회")
    @Transactional(readOnly = true)
    public ActivityRecordSliceResDTO getActivityRecordList(
            Long memberId,
            @LogParam("score_type") ScoreType scoreType,
            @LogParam("page_size") int pageSize,
            @LogParam("page_num") int pageNum
    ) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<ActivityRecord> slice = activityRecordGetService.findActivityRecordList(memberId, scoreType, pageable);

        return ActivityRecordSliceResDTO.from(slice.map(ActivityRecordResDTO::from));
    }

    /** 관리자용 회원의 전체 활동기록 페이징 조회 */
    @Transactional(readOnly = true)
    public AdminActivityRecordSliceResDTO getAdminActivityRecordList(Long memberId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<ActivityRecord> slice = activityRecordGetService.findAllActiveByMemberId(memberId, pageable);

        return AdminActivityRecordSliceResDTO.from(slice.map(AdminActivityRecordResDTO::from));
    }

    /** 관리자용 팀의 전체 활동기록 페이징 조회 */
    @Transactional(readOnly = true)
    public AdminActivityRecordSliceResDTO getAdminTeamActivityRecordList(Long teamId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<ActivityRecord> slice = activityRecordGetService.findAllActiveByTeamId(teamId, pageable);

        return AdminActivityRecordSliceResDTO.from(slice.map(AdminActivityRecordResDTO::from));
    }

    /** 활동기록 수정 (activityType, activityDate) */
    @Transactional
    public void patchActivityRecord(Long activityRecordId, ActivityRecordPatchReqDTO dto) {
        // 행 잠금 조회로 동시 삭제/수정을 직렬화 — 락 없는 조회 후 isDeleted 검사는
        // 선행 트랜잭션의 커밋을 보지 못해 점수 보정이 중복 적용될 수 있다
        ActivityRecord record = activityRecordGetService.findByIdForUpdate(activityRecordId);
        validateNotDeleted(record);

        if (dto.activityType() != null) {
            // 단일 델타 보정으로는 상·벌점 누적합 두 컬럼을 맞출 수 없으므로, 구(scoreType, appliedScore)를 되돌리고 신을 반영한다.
            ScoreType oldScoreType = record.getScoreType();
            BigDecimal oldAppliedScore = record.getAppliedScore();
            activityRecordPatchService.updateActivityType(record, dto.activityType());

            PersonalActivityScore score = findScoreByRecord(record);
            score.applyDelta(oldAppliedScore.negate(), oldScoreType);
            score.applyDelta(record.getAppliedScore(), record.getScoreType());
        }

        if (dto.activityDate() != null) {
            activityRecordPatchService.updateActivityDate(record, dto.activityDate());
        }
    }

    /** 활동기록 소프트 삭제 */
    @Transactional
    public void deleteActivityRecord(Long activityRecordId) {
        // 행 잠금 조회로 동시 삭제를 직렬화 — 락 없는 조회 후 isDeleted 검사는
        // 선행 트랜잭션의 커밋을 보지 못해 점수가 이중 차감될 수 있다
        ActivityRecord record = activityRecordGetService.findByIdForUpdate(activityRecordId);
        validateNotDeleted(record);

        activityRecordDeleteService.softDelete(record);

        PersonalActivityScore score = findScoreByRecord(record);
        score.applyDelta(record.getAppliedScore().negate(), record.getScoreType());
    }

    /** 모든 활동 종류 조회 */
    public List<ActivityCategoryDetailResDTO> getAllActivityTypeInformation() {
        return ActivityCategory.getDetailDtoList();
    }

    /** 모든 활동 카테고리 조회 */
    public List<ActivityCategoryResDTO> getAllActivityCategoriesInformation() {
        return ActivityCategory.getDtoList();
    }

    /** 특정 카테고리의 활동 종류 조회 */
    public ActivityCategoryDetailResDTO getActivityTypeInformationByCategory(String category) {
        ActivityCategory activityCategory = ActivityCategory.valueOf(category);
        return ActivityType.getDtoListByCategory(activityCategory);
    }

    /** 활동기록의 대상(개인/팀)에 해당하는 점수 엔티티 조회 (점수 갱신용 — 행 잠금) */
    private PersonalActivityScore findScoreByRecord(ActivityRecord record) {
        if (record.getTeamId() != null) {
            return personalScoreGetService.getTeamScoreListByIdsForUpdate(List.of(record.getTeamId())).get(0);
        }
        return personalScoreGetService.getPersonalScoreForUpdate(record.getMemberId());
    }

    /** 이미 삭제된 활동기록 검증 */
    private void validateNotDeleted(ActivityRecord record) {
        if (record.isDeleted()) {
            throw new ActivityRecordAlreadyDeletedException();
        }
    }

}
