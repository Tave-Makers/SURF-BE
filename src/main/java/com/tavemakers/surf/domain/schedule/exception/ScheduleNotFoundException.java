package com.tavemakers.surf.domain.schedule.exception;

import static com.tavemakers.surf.domain.schedule.exception.ErrorMessage.SCHEDULE_NOT_FOUND;

import com.tavemakers.surf.global.common.exception.BaseException;

public class ScheduleNotFoundException extends BaseException {
    public ScheduleNotFoundException(){
        super(SCHEDULE_NOT_FOUND.getStatus(), SCHEDULE_NOT_FOUND.getMessage());
    }
}
