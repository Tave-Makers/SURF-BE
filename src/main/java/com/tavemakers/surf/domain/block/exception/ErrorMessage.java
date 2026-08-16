package com.tavemakers.surf.domain.block.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [차단]입니다."),
    BLOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 차단한 [회원]입니다."),
    BLOCK_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신은 차단할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
