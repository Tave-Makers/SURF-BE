package com.tavemakers.surf.domain.report.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.report.exception.ErrorMessage.REPORT_RATE_LIMIT_EXCEEDED;

public class ReportRateLimitExceededException extends BaseException {

    public ReportRateLimitExceededException() {
        super(REPORT_RATE_LIMIT_EXCEEDED.getStatus(), REPORT_RATE_LIMIT_EXCEEDED.getMessage());
    }
}
