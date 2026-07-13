package com.tavemakers.surf.domain.comment.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [댓글]입니다."),
    NOT_MY_COMMENT(HttpStatus.FORBIDDEN, "본인의 [댓글]이 아닙니다."),
    INVALID_BLANK_COMMENT(HttpStatus.BAD_REQUEST, "[댓글] 내용은 공백일 수 없습니다."),
    ALREADY_DELETED_COMMENT(HttpStatus.BAD_REQUEST, "이미 삭제된 [댓글]입니다."),
    INVALID_MENTION_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "회원 멘션 시 이름은 두 글자 이상 입력해주세요."),
    CANNOT_LIKE_DELETED_COMMENT(HttpStatus.BAD_REQUEST, "삭제된 [댓글]에는 좋아요를 누를 수 없습니다."),
    INVALID_REPLY(HttpStatus.BAD_REQUEST, "[대댓글]은 부모 댓글 작성자를 자동 멘션해야 합니다."),
    COMMENT_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요를 누른 [댓글]입니다."),
    DUPLICATE_COMMENT(HttpStatus.CONFLICT, "같은 내용의 [댓글]이 방금 등록되었습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

}