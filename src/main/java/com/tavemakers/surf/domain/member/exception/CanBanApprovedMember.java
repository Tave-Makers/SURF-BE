package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.CAN_BAN_APPROVED_MEMBER;

public class CanBanApprovedMember extends BaseException {

    public CanBanApprovedMember() {
        super(CAN_BAN_APPROVED_MEMBER.getStatus(), CAN_BAN_APPROVED_MEMBER.getMessage());
    }
}
