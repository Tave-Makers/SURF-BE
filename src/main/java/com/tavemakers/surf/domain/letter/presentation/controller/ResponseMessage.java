package com.tavemakers.surf.domain.letter.presentation.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    LETTER_CREATED("[쪽지]가 성공적으로 전송되었습니다."),
    LETTER_SENT_READ("[쪽지] 보낸 목록이 성공적으로 조회되었습니다.");

    private final String message;
}
