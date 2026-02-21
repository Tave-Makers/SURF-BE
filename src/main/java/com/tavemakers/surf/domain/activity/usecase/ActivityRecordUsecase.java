package com.tavemakers.surf.domain.activity.usecase;

import com.tavemakers.surf.domain.activity.dto.request.ActivityRecordReqDTO;
import com.tavemakers.surf.domain.activity.dto.request.ActivityRecordReqDTOV2;
import com.tavemakers.surf.domain.activity.dto.response.ActivityCategoryDetailResDTO;
import com.tavemakers.surf.domain.activity.dto.response.ActivityCategoryResDTO;
import com.tavemakers.surf.domain.activity.dto.response.ActivityRecordResDTO;
import com.tavemakers.surf.domain.activity.dto.response.ActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityCategory;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.domain.activity.service.ActivityRecordGetService;
import com.tavemakers.surf.domain.activity.service.ActivityRecordCreateService;
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
    private final PersonalScoreGetService personalScoreGetService;
    private final ScoreCalculator scoreCalculator;

    /** 다수 회원의 활동기록 생성 및 점수 반영 */
    @Transactional
    public void createActivityRecordList(ActivityRecordReqDTO dto) {
        // 다수의 활동 점수 -> 감점 + 가점 -> 누적합과 함께 활동기록 생성
        List<PersonalActivityScore> scoreList = personalScoreGetService.getPersonalScoreList(dto.memberIdList());
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
        ActivityType activityType = dto.activityName();

        if (dto.isTeam()) {
            List<PersonalActivityScore> teamScoreList = personalScoreGetService.getTeamScoreList(dto.teamIdList());
            List<ActivityRecord> recordList = teamScoreList.stream()
                    .map(teamScore -> {
                        BigDecimal prefixSum = teamScore.updateScore(activityType);
                        return ActivityRecord.ofTeam(teamScore.getTeam().getId(), dto, prefixSum);
                            }
                    ).toList();
            activityRecordCreateService.saveActivityRecordList(recordList);
            return;
        }

        List<PersonalActivityScore> scoreList = personalScoreGetService.getPersonalScoreList(dto.memberIdList());
        List<ActivityRecord> recordList = scoreList.stream()
                .map(personalScore -> {
                            BigDecimal prefixSum = personalScore.updateScore(activityType);
                            return ActivityRecord.ofPersonal(personalScore.getMember().getId(), dto, prefixSum);
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

}
