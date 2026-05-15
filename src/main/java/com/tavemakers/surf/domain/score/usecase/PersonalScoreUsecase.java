package com.tavemakers.surf.domain.score.usecase;

import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityRecordSummaryResDTO;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.mapper.ActivityRecordMapper;
import com.tavemakers.surf.domain.activity.service.activityRecord.ActivityRecordGetService;
import com.tavemakers.surf.domain.score.dto.response.PersonalScoreWithPinnedResDto;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PersonalScoreUsecase {

    private final PersonalScoreGetService personalScoreGetService;
    private final ActivityRecordGetService activityRecordGetService;
    private final ActivityRecordMapper activityRecordMapper;
    private final LogEventEmitter logEventEmitter;

    /** 회원의 개인 점수와 고정 활동기록 조회 */
    public PersonalScoreWithPinnedResDto findPersonalScoreAndPinned(Long memberId) {
        PersonalActivityScore personalScore = personalScoreGetService.getPersonalScore(memberId);

        List<ActivityRecord> list = activityRecordGetService.findAllByMemberId(memberId);
        ActivityRecordSummaryResDTO dto = activityRecordMapper.mapPinnedActivityRecord(list);

        List<String> topTypes = list.stream()
                .map(record -> record.getActivityType().name())
                .limit(5)
                .toList();

        logEventEmitter.emit("personal.score.pinned5", Map.of(
                "score", personalScore.getScore(),
                "top_types", topTypes
        ));

        return PersonalScoreWithPinnedResDto.of(personalScore.getScore(), dto);
    }

}
