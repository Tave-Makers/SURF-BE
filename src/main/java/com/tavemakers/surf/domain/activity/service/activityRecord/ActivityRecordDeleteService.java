package com.tavemakers.surf.domain.activity.service.activityRecord;

import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityRecordDeleteService {

    /** 활동기록 소프트 삭제 */
    public void softDelete(ActivityRecord record) {
        record.softDelete();
    }

}
