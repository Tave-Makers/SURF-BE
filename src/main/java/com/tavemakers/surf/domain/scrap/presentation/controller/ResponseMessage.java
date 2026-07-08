package com.tavemakers.surf.domain.scrap.presentation.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    SCRAP_CREATED("[스크랩]이 성공적으로 생성되었습니다."),
    SCRAP_DELETED("[스크랩]이 성공적으로 삭제되었습니다."),
    MY_SCRAP_LIST_READ("[스크랩] 목록이 성공적으로 조회되었습니다.");

    private final String message;

}