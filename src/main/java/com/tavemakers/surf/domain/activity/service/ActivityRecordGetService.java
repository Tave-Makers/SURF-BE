package com.tavemakers.surf.domain.activity.service;

import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.domain.activity.exception.ActivityRecordNotFoundException;
import com.tavemakers.surf.domain.activity.repository.ActivityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityRecordGetService {

    private final ActivityRecordRepository activityRecordRepository;

    /** 회원의 활동기록 목록을 점수 유형별 페이징 조회 */
    public Slice<ActivityRecord> findActivityRecordList(Long memberId,ScoreType scoreType, Pageable pageable) {
        return activityRecordRepository.findActivityRecordListByMemberId(memberId, scoreType, pageable);
    }

    /** 회원의 전체 활동기록 조회 */
    public List<ActivityRecord> findAllByMemberId(Long memberId) {
        return activityRecordRepository.findByMemberIdAndIsDeleted(memberId, false);
    }

    /** 활동기록 단건 조회 */
    public ActivityRecord findById(Long activityRecordId) {
        return activityRecordRepository.findById(activityRecordId)
                .orElseThrow(ActivityRecordNotFoundException::new);
    }

    /** 회원의 전체 활동기록 페이징 조회 (scoreType 필터 없이) */
    public Slice<ActivityRecord> findAllActiveByMemberId(Long memberId, Pageable pageable) {
        return activityRecordRepository.findAllActiveByMemberId(memberId, pageable);
    }

    /** 팀의 전체 활동기록 페이징 조회 */
    public Slice<ActivityRecord> findAllActiveByTeamId(Long teamId, Pageable pageable) {
        return activityRecordRepository.findAllActiveByTeamId(teamId, pageable);
    }

    /** 다수 회원의 상/벌점 집계 조회 */
    public Map<Long, Map<ScoreType, BigDecimal>> getScoreAggregation(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<ScoreType, BigDecimal>> result = new HashMap<>();
        List<Object[]> rows = activityRecordRepository.findScoreAggregationByMemberIds(memberIds);

        for (Object[] row : rows) {
            Long memberId = (Long) row[0];
            ScoreType scoreType = (ScoreType) row[1];
            BigDecimal sum = (BigDecimal) row[2];

            result.computeIfAbsent(memberId, k -> new EnumMap<>(ScoreType.class))
                    .put(scoreType, sum);
        }

        return result;
    }

}
