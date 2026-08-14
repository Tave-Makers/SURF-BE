package com.tavemakers.surf.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportTargetType {
    POST("게시글"),
    COMMENT("댓글"),
    PROFILE("프로필");

    private final String description;
}
