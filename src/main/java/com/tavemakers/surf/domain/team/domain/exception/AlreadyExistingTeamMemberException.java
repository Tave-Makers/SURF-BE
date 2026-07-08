package com.tavemakers.surf.domain.team.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.team.domain.exception.ErrorMessage.ALREADY_EXISTING_TEAM_MEMBER;

public class AlreadyExistingTeamMemberException extends BaseException {
    public AlreadyExistingTeamMemberException() {
        super(ALREADY_EXISTING_TEAM_MEMBER.getStatus(), ALREADY_EXISTING_TEAM_MEMBER.getMessage());
    }
}