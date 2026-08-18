package com.tavemakers.surf.domain.moderation.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.moderation.exception.ErrorMessage.MODERATION_DICTIONARY_EMPTY;

/**
 * 시드 이후에도 금칙어가 0건일 때 기동을 실패시키기 위한 예외.
 * 마스킹이 소리 없이 꺼진 채 배포되는 것을 막는다.
 */
public class ModerationDictionaryEmptyException extends BaseException {
    public ModerationDictionaryEmptyException() {
        super(MODERATION_DICTIONARY_EMPTY.getStatus(), MODERATION_DICTIONARY_EMPTY.getMessage());
    }
}
