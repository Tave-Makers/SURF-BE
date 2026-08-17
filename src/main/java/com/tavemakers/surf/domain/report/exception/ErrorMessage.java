package com.tavemakers.surf.domain.report.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    SELF_REPORT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인 자신은 신고할 수 없습니다."),
    REPORT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "신고는 5분 동안 최대 3회까지 가능합니다. 잠시 후 다시 시도해주세요."),
    REPORT_SNAPSHOT_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "신고 대상 정보를 저장하는 중 오류가 발생했습니다."),
    REPORT_SNAPSHOT_DESERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "신고 대상 정보를 조회하는 중 오류가 발생했습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신고입니다."),
    INVALID_REPORT_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "신고 상태는 PENDING에서 RESOLVED 또는 REJECTED로만 변경할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
