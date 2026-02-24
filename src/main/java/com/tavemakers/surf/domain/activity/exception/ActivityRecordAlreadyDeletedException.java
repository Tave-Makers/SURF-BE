package com.tavemakers.surf.domain.activity.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.activity.exception.ErrorMessage.ACTIVITY_RECORD_ALREADY_DELETED;

public class ActivityRecordAlreadyDeletedException extends BaseException {
    /** 이미 삭제된 활동기록 예외 생성 */
    public ActivityRecordAlreadyDeletedException() {
        super(ACTIVITY_RECORD_ALREADY_DELETED.getStatus(), ACTIVITY_RECORD_ALREADY_DELETED.getMessage());
    }
}
