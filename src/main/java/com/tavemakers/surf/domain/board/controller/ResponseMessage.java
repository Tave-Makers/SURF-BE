package com.tavemakers.surf.domain.board.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    BOARD_CREATED("[게시판]이 성공적으로 생성되었습니다."),
    BOARD_UPDATED("[게시판]이 성공적으로 수정되었습니다."),
    BOARD_DELETED("[게시판]이 성공적으로 삭제되었습니다."),
    BOARD_READ("[게시판]이 성공적으로 조회되었습니다."),

    CATEGORY_CREATED("[카테고리]가 성공적으로 생성되었습니다.");

    private final String message;

}