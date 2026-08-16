package com.tavemakers.surf.domain.report.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.report.exception.ErrorMessage.REPORT_NOT_FOUND;

public class ReportNotFoundException extends BaseException {

    public ReportNotFoundException() {
        super(REPORT_NOT_FOUND.getStatus(), REPORT_NOT_FOUND.getMessage());
    }
}
