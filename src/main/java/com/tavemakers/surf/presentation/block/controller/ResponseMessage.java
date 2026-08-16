package com.tavemakers.surf.presentation.block.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    BLOCK_CREATED("[차단]이 성공적으로 등록되었습니다."),
    BLOCK_DELETED("[차단]이 성공적으로 해제되었습니다."),
    MY_BLOCK_LIST_READ("[차단] 목록이 성공적으로 조회되었습니다."),
    ADMIN_BLOCK_LIST_READ("[차단] 관계 목록이 성공적으로 조회되었습니다."),
    ADMIN_BLOCK_RELEASED("[차단]이 관리자에 의해 성공적으로 해제되었습니다.");

    private final String message;

}
