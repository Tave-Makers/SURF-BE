package com.tavemakers.surf.domain.team.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.team.exception.ErrorMessage.CANNOT_REMOVE_TEAM_LEADER;

public class CannotRemoveTeamLeaderException extends BaseException {
    public CannotRemoveTeamLeaderException() {
        super(CANNOT_REMOVE_TEAM_LEADER.getStatus(), CANNOT_REMOVE_TEAM_LEADER.getMessage());
    }
}
