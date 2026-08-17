package com.tavemakers.surf.domain.report.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.report.exception.ErrorMessage.INVALID_REPORT_STATUS_CHANGE;

public class InvalidReportStatusChangeException extends BaseException {

    public InvalidReportStatusChangeException() {
        super(INVALID_REPORT_STATUS_CHANGE.getStatus(), INVALID_REPORT_STATUS_CHANGE.getMessage());
    }
}
