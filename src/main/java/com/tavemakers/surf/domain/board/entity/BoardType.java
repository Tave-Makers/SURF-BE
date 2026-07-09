package com.tavemakers.surf.domain.board.entity;

public enum BoardType {
    NOTICE,   // 공지사항 - 관리자만 게시글 작성 가능
    GENERAL,  // 일반 게시판 - 일반 회원도 게시글 작성 가능
}