package com.tavemakers.surf.domain.score.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    SCORE_AND_PINNED_READ("[개인활동점수]와 [고정활동내역]을 조회합니다."),
    MEMBER_SCORE_RANKING_READ("[개인별 상/벌점 현황]을 조회합니다."),
    TEAM_SCORE_RANKING_READ("[팀별 상/벌점 현황]을 조회합니다."),
    TEAM_MEMBER_SCORE_READ("[팀 멤버 점수 현황]을 조회했습니다."),
    ;

    private final String message;

}
