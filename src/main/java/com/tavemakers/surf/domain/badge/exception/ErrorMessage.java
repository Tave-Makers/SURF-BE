package com.tavemakers.surf.domain.badge.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [활동 뱃지]입니다."),
    MEMBER_BADGE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 [활동 뱃지]를 보유한 [회원]이 존재합니다."),
    MEMBER_BADGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "회수 대상 중 해당 [활동 뱃지]를 가지고 있지 않은 [회원]이 포함되어 있습니다.");

    private final HttpStatus status;
    private final String message;
}