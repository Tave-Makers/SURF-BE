package com.tavemakers.surf.presentation.report.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    REPORT_CREATED("[신고]가 성공적으로 접수되었습니다.");

    private final String message;
}
