package com.tavemakers.surf.domain.team.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [팀]입니다."),
    TEAM_MEMBER_DUPLICATED(HttpStatus.BAD_REQUEST, "리스트 내에 중복된 [팀원]ID가 존재합니다."),
    TEAM_LEADER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [팀장]입니다."),
    TEAM_LEADER_NOT_IN_MEMBER(HttpStatus.BAD_REQUEST, "팀에 존재하지 않는 [팀장]입니다."),

    TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [팀원]입니다."),
    ALREADY_EXISTING_TEAM_MEMBER(HttpStatus.BAD_REQUEST, "이미 존재하는 [팀원]입니다."),
    CANNOT_REMOVE_TEAM_LEADER(HttpStatus.BAD_REQUEST, "[팀장]은 팀에서 제거할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
