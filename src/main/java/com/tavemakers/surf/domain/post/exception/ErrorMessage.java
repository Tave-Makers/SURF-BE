package com.tavemakers.surf.domain.post.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [게시글]입니다."),
    POST_ALREADY_DELETED(HttpStatus.NOT_FOUND, "이미 삭제된 [게시글]입니다."),

    POST_IMAGE_EMPTY(HttpStatus.NOT_FOUND, "[이미지 목록]이 비어있습니다."),
    POST_DELETED_DENIED(HttpStatus.UNAUTHORIZED, "[게시글]을 삭제할 권한이 없습니다."),

    POST_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [첨부파일]입니다."),
    POST_FILE_DELETE_DENIED(HttpStatus.UNAUTHORIZED, "[첨부파일]을 삭제할 권한이 없습니다."),

    BOARD_WRITE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "[공지사항] 게시판은 관리자만 게시글을 작성할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

}
