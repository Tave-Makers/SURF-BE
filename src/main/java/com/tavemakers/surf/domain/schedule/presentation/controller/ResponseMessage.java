package com.tavemakers.surf.domain.schedule.presentation.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    SCHEDULE_CREATED("[일정]이 성공적으로 생성되었습니다"),
    SCHEDULE_UPDATED("[일정]이 성공적으로 수정되었습니다."),
    SCHEDULE_CALENDAR_READ("[일정]이 성공적으로 월별 조회되었습니다."),
    SCHEDULE_DELETED("[일정]이 성공적으로 삭제되었습니다."),
    SCHEDULE_POST_READ("[게시글]과 매핑된 [일정]이 성공적으로 조회되었습니다."),
    SCHEDULE_READ("[일정]이 성공적으로 조회되었습니다.");

    private final String message;

}
