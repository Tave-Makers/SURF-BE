package com.tavemakers.surf.domain.post.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.exception.ErrorMessage.POST_FILE_MISMATCH;

public class PostFileMismatchException extends BaseException {
    public PostFileMismatchException() {
        super(POST_FILE_MISMATCH.getStatus(), POST_FILE_MISMATCH.getMessage());
    }
}
