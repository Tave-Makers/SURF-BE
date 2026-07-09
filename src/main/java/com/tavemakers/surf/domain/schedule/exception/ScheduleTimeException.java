package com.tavemakers.surf.domain.schedule.exception;

import static com.tavemakers.surf.domain.schedule.exception.ErrorMessage.SCHEDULE_TIME_ERROR;

import com.tavemakers.surf.global.common.exception.BaseException;

public class ScheduleTimeException extends BaseException {
    public ScheduleTimeException() {
        super(SCHEDULE_TIME_ERROR.getStatus(), SCHEDULE_TIME_ERROR.getMessage());
    }
}
