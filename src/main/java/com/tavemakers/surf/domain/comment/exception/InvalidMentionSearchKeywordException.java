package com.tavemakers.surf.domain.comment.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.comment.exception.ErrorMessage.INVALID_MENTION_SEARCH_KEYWORD;

public class InvalidMentionSearchKeywordException extends BaseException {
    public InvalidMentionSearchKeywordException() {
        super(INVALID_MENTION_SEARCH_KEYWORD.getStatus(), INVALID_MENTION_SEARCH_KEYWORD.getMessage());
    }
}
