package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.CAREER_NOT_FOUND;

public class CareerNotFoundException extends BaseException {

    //기본 생성자
    public CareerNotFoundException() {
        super(CAREER_NOT_FOUND.getStatus(), CAREER_NOT_FOUND.getMessage());
    }

    //동적 메시지를 추가하기 위한 생성자
    public CareerNotFoundException(String detailMessage) {
        super(CAREER_NOT_FOUND.getStatus(), CAREER_NOT_FOUND.getMessage() + " " + detailMessage);
    }
}
