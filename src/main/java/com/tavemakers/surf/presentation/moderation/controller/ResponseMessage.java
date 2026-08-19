package com.tavemakers.surf.presentation.moderation.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    MODERATION_TERM_CREATED("[금칙어 사전] 항목이 성공적으로 등록되었습니다."),
    MODERATION_TERM_READ("[금칙어 사전] 항목이 성공적으로 조회되었습니다."),
    MODERATION_TERM_DELETED("[금칙어 사전] 항목이 성공적으로 삭제되었습니다.");

    private final String message;

}
