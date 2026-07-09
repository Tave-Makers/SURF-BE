package com.tavemakers.surf.domain.activity.service.activityRecord;

import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ActivityRecordPatchService {

    /** 활동 유형 변경 및 점수 차이 반환 */
    public BigDecimal updateActivityType(ActivityRecord record, ActivityType newActivityType) {
        return record.updateActivityType(newActivityType);
    }

    /** 활동 날짜 변경 */
    public void updateActivityDate(ActivityRecord record, LocalDate newActivityDate) {
        record.updateActivityDate(newActivityDate);
    }

}
