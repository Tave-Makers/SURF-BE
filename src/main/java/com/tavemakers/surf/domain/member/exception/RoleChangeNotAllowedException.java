package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.ROLE_CHANGE_NOT_ALLOWED;

public class RoleChangeNotAllowedException extends BaseException {

    public RoleChangeNotAllowedException() {
        super(ROLE_CHANGE_NOT_ALLOWED.getStatus(), ROLE_CHANGE_NOT_ALLOWED.getMessage());
    }
}
