package com.tavemakers.surf.domain.moderation.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    MODERATION_TERM_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 [금칙어 사전] 항목입니다."),
    MODERATION_TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [금칙어 사전] 항목입니다."),
    MODERATION_DICTIONARY_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR,
            "[금칙어 사전]이 비어 있습니다. 시드 파일(moderation/badwords.txt) 또는 moderation_term 테이블을 확인하세요.");

    private final HttpStatus status;
    private final String message;

}
