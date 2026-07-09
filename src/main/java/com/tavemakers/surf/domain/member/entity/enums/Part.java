package com.tavemakers.surf.domain.member.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Part {

    BACKEND("백엔드"),
    WEB_FRONTEND("웹 프론트엔드"),
    APP_FRONTEND("앱 프론트엔드"),
    DESIGN("디자인"),
    DATA_ANALYSIS("데이터 분석"),
    DEEP_LEARNING("딥러닝");

    private final String displayName;
}
