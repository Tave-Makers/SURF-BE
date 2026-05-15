package com.tavemakers.surf.domain.activity.usecase;

import com.tavemakers.surf.domain.activity.dto.activityRecord.request.ActivityRecordPatchReqDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.request.ActivityRecordReqDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.request.ActivityRecordReqDTOV2;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityCategoryDetailResDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityCategoryResDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityRecordResDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.AdminActivityRecordResDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.AdminActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityCategory;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.domain.activity.exception.ActivityRecordAlreadyDeletedException;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordDeleteService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordGetService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordCreateService;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordPatchService;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import com.tavemakers.surf.domain.score.utils.ScoreCalculator;
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
        // 다수의 활동 점수 -> 감점 + 가점 -> 누적합과 함께 활동기록 생성
        List<PersonalActivityScore> scoreList = personalScoreGetService.getPersonalScoreListByIds(dto.memberIdList());
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

            logEventEmitter.emit("activity.record.create", Map.of(
                    "member_id_list_count",
                    dto.isTeam()
                            ? dto.teamIdList().size()
                            : dto.memberIdList().size(),

                    "activity_name",
                    dto.activityName().name(),

                    "activity_date",
                    dto.activityDate()
            ));

            ActivityType activityType = dto.activityName();

            if (dto.isTeam()) {

                List<PersonalActivityScore> teamScoreList =
                        personalScoreGetService.getTeamScoreListByIds(dto.teamIdList());

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

                return;
            }

            List<PersonalActivityScore> scoreList =
                    personalScoreGetService.getPersonalScoreListByIds(dto.memberIdList());

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
        ActivityRecord record = activityRecordGetService.findById(activityRecordId);
        validateNotDeleted(record);

        if (dto.activityType() != null) {
            BigDecimal scoreDelta = activityRecordPatchService.updateActivityType(record, dto.activityType());
            PersonalActivityScore score = findScoreByRecord(record);
            score.updateScore(scoreDelta);
        }

        if (dto.activityDate() != null) {
            activityRecordPatchService.updateActivityDate(record, dto.activityDate());
        }
    }

    /** 활동기록 소프트 삭제 */
    @Transactional
    public void deleteActivityRecord(Long activityRecordId) {
        ActivityRecord record = activityRecordGetService.findById(activityRecordId);
        validateNotDeleted(record);

        activityRecordDeleteService.softDelete(record);

        PersonalActivityScore score = findScoreByRecord(record);
        score.updateScore(record.getAppliedScore().negate());
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

    /** 활동기록의 대상(개인/팀)에 해당하는 점수 엔티티 조회 */
    private PersonalActivityScore findScoreByRecord(ActivityRecord record) {
        if (record.getTeamId() != null) {
            return personalScoreGetService.getTeamScoreListByIds(List.of(record.getTeamId())).get(0);
        }
        return personalScoreGetService.getPersonalScore(record.getMemberId());
    }

    /** 이미 삭제된 활동기록 검증 */
    private void validateNotDeleted(ActivityRecord record) {
        if (record.isDeleted()) {
            throw new ActivityRecordAlreadyDeletedException();
        }
    }

}
