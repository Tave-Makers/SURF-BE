package com.tavemakers.surf.domain.activity.service.activityRecord;

import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.repository.ActivityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityRecordCreateService {

    private final ActivityRecordRepository activityRecordRepository;

    /** 활동기록 목록 일괄 저장 */
    public void saveActivityRecordList(List<ActivityRecord> recordList) {
        activityRecordRepository.saveAll(recordList);
    }
}
