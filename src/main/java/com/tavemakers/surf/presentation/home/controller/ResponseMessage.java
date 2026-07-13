package com.tavemakers.surf.presentation.home.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    HOME_PAGE_RENDERED("홈 화면 렌더링에 성공했습니다."),

    HOME_CONTENT_READ("[홈 화면 메시지]를 조회했습니다."),
    HOME_CONTENT_UPSERTED("[홈 화면 메시지]를 수정했습니다."),

    HOME_BANNERS_READ("[홈 배너] 목록을 조회했습니다."),
    HOME_BANNER_CREATED("[홈 배너]를 생성했습니다."),
    HOME_BANNER_DELETED("[홈 배너]를 삭제했습니다."),
    HOME_BANNER_REORDERED("[홈 배너] 순서를 변경했습니다."),
    HOME_BANNER_UPDATED("[홈 배너]를 수정했습니다."),

    HOME_BANNER_STATUS_ACTIVATED("[홈 배너]를 활성화했습니다."),
    HOME_BANNER_STATUS_DEACTIVATED("[홈 배너]를 비활성화했습니다.");

    private final String message;
}
