package com.tavemakers.surf.domain.team.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    TEAM_CREATED("[팀]이 성공적으로 생성되었습니다."),
    TEAM_UPDATED("[팀]이 성공적으로 수정되었습니다."),
    TEAM_DELETED("[팀]이 성공적으로 삭제되었습니다."),
    TEAM_READ("[팀]이 성공적으로 조회되었습니다.");


    private final String message;
}
