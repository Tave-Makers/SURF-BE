package com.tavemakers.surf.domain.score.facade;

import com.tavemakers.surf.domain.activity.dto.response.ActivityRecordSummaryResDTO;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.mapper.ActivityRecordMapper;
import com.tavemakers.surf.domain.activity.service.ActivityRecordGetService;
import com.tavemakers.surf.domain.score.dto.response.PersonalScoreWithPinnedResDto;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PersonalScoreFacade {

    private final PersonalScoreGetService personalScoreGetService;
    private final ActivityRecordGetService activityRecordGetService;
    private final ActivityRecordMapper activityRecordMapper;

    public PersonalScoreWithPinnedResDto findPersonalScoreAndPinned(Long memberId) {
        PersonalActivityScore personalScore = personalScoreGetService.getPersonalScore(memberId);

        List<ActivityRecord> list = activityRecordGetService.findAllByMemberId(memberId);
        ActivityRecordSummaryResDTO dto = activityRecordMapper.mapPinnedActivityRecord(list);

        return PersonalScoreWithPinnedResDto.of(personalScore.getScore(), dto);
    }

}
