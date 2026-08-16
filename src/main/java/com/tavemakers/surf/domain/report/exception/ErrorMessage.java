package com.tavemakers.surf.domain.report.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    SELF_REPORT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인 자신은 신고할 수 없습니다."),
    REPORT_SNAPSHOT_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "신고 대상 정보를 저장하는 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
