package com.tavemakers.surf.presentation.comment.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    COMMENT_CREATED("[댓글]이 성공적으로 생성되었습니다."),
    COMMENT_READ("[댓글]이 성공적으로 조회되었습니다."),
    COMMENT_DELETED("[댓글]이 성공적으로 삭제되었습니다."),

    COMMENT_LIKE_TOGGLED("[댓글 좋아요] 상태가 성공적으로 변경되었습니다."),
    COMMENT_LIKE_COUNT_READ("[댓글 좋아요] 개수가 성공적으로 조회되었습니다."),
    COMMENT_LIKE_STATUS_READ("[댓글 좋아요] 상태가 성공적으로 조회되었습니다."),

    COMMENT_MENTION_SEARCH_SUCCESS("[멘션 검색]이 성공적으로 조회되었습니다."),
    COMMENT_LIKE_MEMBER_LIST_READ("[댓글 좋아요] 누른 회원 목록이 성공적으로 조회되었습니다.");

    private final String message;
}
