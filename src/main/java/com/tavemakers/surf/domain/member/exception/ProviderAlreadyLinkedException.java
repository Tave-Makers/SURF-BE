package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.PROVIDER_ALREADY_LINKED;

/** 기존 회원이 이미 해당 provider의 SocialAccount를 보유할 때 (1 provider = 1 account, 5.A-3). */
public class ProviderAlreadyLinkedException extends BaseException {
    public ProviderAlreadyLinkedException() {
        super(PROVIDER_ALREADY_LINKED.getStatus(), PROVIDER_ALREADY_LINKED.getMessage());
    }
}
