package com.tavemakers.surf.domain.team.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.team.exception.ErrorMessage.TEAM_LEADER_NOT_IN_MEMBER;

public class TeamLeaderNotInMemberException extends BaseException {
    public TeamLeaderNotInMemberException() {
        super(TEAM_LEADER_NOT_IN_MEMBER.getStatus(), TEAM_LEADER_NOT_IN_MEMBER.getMessage());
    }
}