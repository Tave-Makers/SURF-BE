package com.tavemakers.surf.global.common.s3.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.global.common.s3.exception.ErrorMessage.FILE_URL_INVALID;

public class InvalidFileUrlException extends BaseException {
    public InvalidFileUrlException() {
        super(FILE_URL_INVALID.getStatus(), FILE_URL_INVALID.getMessage());
    }
}
