package com.tavemakers.surf.domain.activity.usecase;

import com.tavemakers.surf.domain.activity.dto.request.ActivityRecordPatchReqDTO;
import com.tavemakers.surf.domain.activity.dto.request.ActivityRecordReqDTO;
import com.tavemakers.surf.domain.activity.dto.response.ActivityRecordResDTO;
import com.tavemakers.surf.domain.activity.dto.response.ActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.dto.response.AdminActivityRecordResDTO;
import com.tavemakers.surf.domain.activity.dto.response.AdminActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.domain.activity.exception.ActivityRecordAlreadyDeletedException;
import com.tavemakers.surf.domain.activity.service.ActivityRecordDeleteService;
import com.tavemakers.surf.domain.activity.service.ActivityRecordGetService;
import com.tavemakers.surf.domain.activity.service.ActivityRecordCreateService;
import com.tavemakers.surf.domain.activity.service.ActivityRecordPatchService;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import com.tavemakers.surf.domain.score.utils.ScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityRecordUsecase {

    private final ActivityRecordCreateService activityRecordCreateService;
    private final ActivityRecordGetService activityRecordGetService;
    private final ActivityRecordPatchService activityRecordPatchService;
    private final ActivityRecordDeleteService activityRecordDeleteService;
    private final PersonalScoreGetService personalScoreGetService;
    private final ScoreCalculator scoreCalculator;

    /** 다수 회원의 활동기록 생성 및 점수 반영 */
    @Transactional
    public void createActivityRecordList(ActivityRecordReqDTO dto) {
        // 다수의 활동 점수 -> 감점 + 가점 -> 누적합과 함께 활동기록 생성
        List<PersonalActivityScore> scoreList = personalScoreGetService.getPersonalScoreList(dto.memberIdList());
        List<ActivityRecord> recordList = scoreList.stream()
                .map(personalScore -> {
                            BigDecimal prefixSum = scoreCalculator.calculateScore(personalScore, BigDecimal.valueOf(dto.activityName().getDelta()));
                            return ActivityRecord.of(personalScore.getMember().getId(), dto, prefixSum);
                        }
                ).toList();

        activityRecordCreateService.saveActivityRecordList(recordList);
    }

    /** 회원의 활동기록 목록 페이징 조회 */
    public ActivityRecordSliceResDTO getActivityRecordList(Long memberId, ScoreType scoreType, int pageSize, int pageNum) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<ActivityRecord> slice = activityRecordGetService.findActivityRecordList(memberId, scoreType, pageable);

        return ActivityRecordSliceResDTO.from(slice.map(ActivityRecordResDTO::from));
    }

    /** 관리자용 회원의 전체 활동기록 페이징 조회 */
    public AdminActivityRecordSliceResDTO getAdminActivityRecordList(Long memberId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<ActivityRecord> slice = activityRecordGetService.findAllActiveByMemberId(memberId, pageable);

        return AdminActivityRecordSliceResDTO.from(slice.map(AdminActivityRecordResDTO::from));
    }

    /** 활동기록 수정 (activityType, activityDate) */
    @Transactional
    public void patchActivityRecord(Long activityRecordId, ActivityRecordPatchReqDTO dto) {
        ActivityRecord record = activityRecordGetService.findById(activityRecordId);
        validateNotDeleted(record);

        if (dto.activityType() != null) {
            BigDecimal scoreDelta = activityRecordPatchService.updateActivityType(record, dto.activityType());
            PersonalActivityScore personalScore = personalScoreGetService.getPersonalScore(record.getMemberId());
            personalScore.updateScore(scoreDelta);
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

        PersonalActivityScore personalScore = personalScoreGetService.getPersonalScore(record.getMemberId());
        personalScore.updateScore(record.getAppliedScore().negate());
    }

    /** 이미 삭제된 활동기록 검증 */
    private void validateNotDeleted(ActivityRecord record) {
        if (record.isDeleted()) {
            throw new ActivityRecordAlreadyDeletedException();
        }
    }

}
