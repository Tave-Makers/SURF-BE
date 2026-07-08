package com.tavemakers.surf.domain.team.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.team.domain.exception.ErrorMessage.TEAM_LEADER_NOT_FOUND;

public class TeamLeaderNotFoundException extends BaseException {
    public TeamLeaderNotFoundException() {
        super(TEAM_LEADER_NOT_FOUND.getStatus(), TEAM_LEADER_NOT_FOUND.getMessage());
    }
}