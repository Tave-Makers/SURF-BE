package com.tavemakers.surf.domain.report.repository;

public interface ReportRateLimitRepository {

    /** 5분 동안 최대 3회 신고만 허용한다. */
    void validate(Long memberId);

    /** 성공적으로 접수된 신고만 5분 제한 카운트에 반영한다. */
    void count(Long memberId);
}
