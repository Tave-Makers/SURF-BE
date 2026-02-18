package com.tavemakers.surf.domain.team.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.team.exception.ErrorMessage.TEAM_MEMBER_NOT_FOUND;

public class TeamMemberNotFoundException extends BaseException {
    public TeamMemberNotFoundException() {
        super(TEAM_MEMBER_NOT_FOUND.getStatus(), TEAM_MEMBER_NOT_FOUND.getMessage());
    }
}