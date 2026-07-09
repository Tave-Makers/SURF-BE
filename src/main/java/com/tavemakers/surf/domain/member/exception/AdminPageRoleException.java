package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.ADMIN_PAGE_ROLE_EXCEPTION;

public class AdminPageRoleException extends BaseException {
    public AdminPageRoleException() {
        super(ADMIN_PAGE_ROLE_EXCEPTION.getStatus(), ADMIN_PAGE_ROLE_EXCEPTION.getMessage());
    }
}
