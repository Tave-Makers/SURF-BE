package com.tavemakers.surf.domain.report.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.report.exception.ErrorMessage.REPORT_SNAPSHOT_SERIALIZATION_FAILED;

public class ReportSnapshotSerializationException extends BaseException {
    public ReportSnapshotSerializationException() {
        super(
                REPORT_SNAPSHOT_SERIALIZATION_FAILED.getStatus(),
                REPORT_SNAPSHOT_SERIALIZATION_FAILED.getMessage()
        );
    }
}
