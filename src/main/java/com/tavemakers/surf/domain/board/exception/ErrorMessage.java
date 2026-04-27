package com.tavemakers.surf.domain.board.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [게시판]입니다."),

    CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "[카테고리]는 필수 입력값입니다."),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [카테고리]입니다."),

    BOARD_CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 [게시판]에 이미 존재하는 [슬러그]입니다."),

    INVALID_CATEGORY_MAPPING(HttpStatus.BAD_REQUEST, "유효하지 않은 [게시판]과 [카테고리] 조합입니다.");

    private final HttpStatus status;
    private final String message;

}
