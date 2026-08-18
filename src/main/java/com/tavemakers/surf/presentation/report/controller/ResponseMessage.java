package com.tavemakers.surf.presentation.report.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    REPORT_CREATED("[신고]가 성공적으로 접수되었습니다."),
    REPORT_LIST_READ("[신고 목록]이 성공적으로 조회되었습니다."),
    REPORT_DETAIL_READ("[신고 상세]가 성공적으로 조회되었습니다."),
    REPORT_STATUS_UPDATED("[신고 상태]가 성공적으로 변경되었습니다.");

    private final String message;
}
