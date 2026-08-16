package com.tavemakers.surf.domain.report.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.report.exception.ErrorMessage.REPORT_SNAPSHOT_DESERIALIZATION_FAILED;

public class ReportSnapshotDeserializationException extends BaseException {

    public ReportSnapshotDeserializationException() {
        super(REPORT_SNAPSHOT_DESERIALIZATION_FAILED.getStatus(), REPORT_SNAPSHOT_DESERIALIZATION_FAILED.getMessage());
    }
}
