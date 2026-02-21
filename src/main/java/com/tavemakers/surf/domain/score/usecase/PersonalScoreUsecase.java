package com.tavemakers.surf.domain.score.usecase;

import com.tavemakers.surf.domain.activity.dto.response.ActivityRecordSummaryResDTO;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.mapper.ActivityRecordMapper;
import com.tavemakers.surf.domain.activity.service.ActivityRecordGetService;
import com.tavemakers.surf.domain.score.dto.response.PersonalScoreWithPinnedResDto;
import com.tavemakers.surf.domain.score.dto.response.ScoreDetailResDTO;
import com.tavemakers.surf.domain.score.dto.response.ScoreSliceResDTO;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalScoreUsecase {

    private final PersonalScoreGetService personalScoreGetService;
    private final ActivityRecordGetService activityRecordGetService;
    private final ActivityRecordMapper activityRecordMapper;

    /** 회원의 개인 점수와 고정 활동기록 조회 */
    public PersonalScoreWithPinnedResDto findPersonalScoreAndPinned(Long memberId) {
        // 개인 점수, 고정 활동기록 조회
        PersonalActivityScore personalScore = personalScoreGetService.getPersonalScore(memberId);

        List<ActivityRecord> list = activityRecordGetService.findAllByMemberId(memberId);
        ActivityRecordSummaryResDTO dto = activityRecordMapper.mapPinnedActivityRecord(list);

        return PersonalScoreWithPinnedResDto.of(personalScore.getScore(), dto);
    }

    public ScoreSliceResDTO readPersonalScore(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Slice<ScoreDetailResDTO> slice = personalScoreGetService.getPersonalScoreSlice(pageable)
                .map(ScoreDetailResDTO::from);
        return ScoreSliceResDTO.from(slice);
    }

    public ScoreSliceResDTO readTeamScore(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Slice<ScoreDetailResDTO> slice = personalScoreGetService.getTeamScoreSlice(pageable)
                .map(ScoreDetailResDTO::from);
        return ScoreSliceResDTO.from(slice);
    }


}
