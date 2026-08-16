package com.tavemakers.surf.domain.report.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.report.exception.ErrorMessage.SELF_REPORT_NOT_ALLOWED;

public class SelfReportNotAllowedException extends BaseException {
    public SelfReportNotAllowedException() {
        super(SELF_REPORT_NOT_ALLOWED.getStatus(), SELF_REPORT_NOT_ALLOWED.getMessage());
    }
}
