package com.tavemakers.surf.domain.team.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.team.exception.ErrorMessage.TEAM_MEMBER_DUPLICATED;

public class TeamMemberDuplicatedException extends BaseException {
    public TeamMemberDuplicatedException() {
        super(TEAM_MEMBER_DUPLICATED.getStatus(), TEAM_MEMBER_DUPLICATED.getMessage());
    }
}