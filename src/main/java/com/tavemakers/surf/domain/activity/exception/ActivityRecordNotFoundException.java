package com.tavemakers.surf.domain.activity.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.activity.exception.ErrorMessage.ACTIVITY_RECORD_NOT_FOUND;

public class ActivityRecordNotFoundException extends BaseException {
    /** 활동기록을 찾을 수 없는 경우 예외 생성 */
    public ActivityRecordNotFoundException() {
        super(ACTIVITY_RECORD_NOT_FOUND.getStatus(), ACTIVITY_RECORD_NOT_FOUND.getMessage());
    }
}
