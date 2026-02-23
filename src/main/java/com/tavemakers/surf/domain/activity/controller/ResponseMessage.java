package com.tavemakers.surf.domain.activity.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    ACTIVITY_RECORD_READ("[활동 기록]을 조회했습니다."),
    ACTIVITY_RECORD_CREATED("[활동 기록]을 적용했습니다."),
    ACTIVITY_RECORD_UPDATED("[활동 기록]을 수정했습니다."),
    ACTIVITY_RECORD_DELETED("[활동 기록]을 삭제했습니다."),
    TEAM_MEMBER_SCORE_READ("[팀 멤버 점수 현황]을 조회했습니다."),
    ALL_ACTIVITY_TYPE_READ("[모든 활동 종류]를 조회합니다."),
    ALL_ACTIVITY_CATEGORY_READ("[모든 활동 카테고리]를 조회합니다."),
    SPECIFIC_ACTIVITY_CATEGORY_READ("[모든 활동 카테고리]를 조회합니다."),
    ;

    private final String message;
}
