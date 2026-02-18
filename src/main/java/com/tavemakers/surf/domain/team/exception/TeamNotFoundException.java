package com.tavemakers.surf.domain.team.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.team.exception.ErrorMessage.TEAM_NOT_FOUND;

public class TeamNotFoundException extends BaseException {
    public TeamNotFoundException() {
        super(TEAM_NOT_FOUND.getStatus(), TEAM_NOT_FOUND.getMessage());
    }
}