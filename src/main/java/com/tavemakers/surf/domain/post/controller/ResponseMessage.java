package com.tavemakers.surf.domain.post.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    POST_CREATED("[게시글]이 성공적으로 생성되었습니다."),
    POST_UPDATED("[게시글]이 성공적으로 수정되었습니다."),
    POST_DELETED("[게시글]이 성공적으로 삭제되었습니다."),
    POST_READ("[게시글]이 성공적으로 조회되었습니다."),
    POST_FILE_DELETED("[게시글] 첨부파일이 성공적으로 삭제되었습니다."),
    MY_POSTS_READ("내가 작성한 [게시글] 목록을 성공적으로 조회했습니다."),
    POSTS_BY_BOARD_READ("[게시판]별 [게시글] 목록을 성공적으로 조회했습니다."),
    POSTS_BY_MEMBER_READ("특정 회원이 작성한 [게시글] 목록을 성공적으로 조회했습니다."),

    POST_LIKE_CREATED("[게시글] 좋아요가 성공적으로 추가되었습니다."),
    POST_LIKE_DELETED("[게시글] 좋아요가 성공적으로 취소되었습니다."),
    POST_LIKES_READ("[게시글] 좋아요 리스트가 성공적으로 조회되었습니다."),

    SEARCH_COMPLETED("검색이 성공적으로 완료되었습니다."),
    RECENT_SEARCH_READ("최근 검색어 목록이 성공적으로 조회되었습니다."),
    RECENT_SEARCH_DELETED("최근 검색어가 성공적으로 삭제되었습니다."),
    RECENT_SEARCH_ONE_DELETED("최근 검색어 한 개가 성공적으로 삭제되었습니다.");

    private final String message;

}