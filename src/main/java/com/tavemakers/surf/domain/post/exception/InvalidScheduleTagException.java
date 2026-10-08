package com.tavemakers.surf.domain.post.exception;

import com.tavemakers.surf.global.common.exception.BaseException;
import static com.tavemakers.surf.domain.post.exception.ErrorMessage.INVALID_SCHEDULE_TAG;

/** 공지사항 외 게시글에 일정 태그를 시도한 경우 */
public class InvalidScheduleTagException extends BaseException {
    public InvalidScheduleTagException() {
        super(INVALID_SCHEDULE_TAG.getStatus(), INVALID_SCHEDULE_TAG.getMessage());
    }
}
