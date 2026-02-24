package com.tavemakers.surf.domain.badge.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    // 배지 관리
    BADGE_CREATED("[활동 배지]가 성공적으로 생성되었습니다."),
    BADGE_LIST_READ("[활동 배지] 목록이 성공적으로 조회되었습니다."),
    BADGE_UPDATED("[활동 배지]가 성공적으로 수정되었습니다."),
    BADGE_DELETED("[활동 배지]가 성공적으로 삭제되었습니다."),

    BADGE_MEMBER_LIST_READ("해당 [활동 배지]를 받은 [회원 목록]이 성공적으로 조회되었습니다."),

    BADGE_ASSIGNED("선택한 모든 [회원]들에게 [활동 배지]가 성공적으로 부여되었습니다."),
    BADGE_REVOKED("선택한 모든 [회원]들의 [활동 배지]가 성공적으로 회수되었습니다."),

    // 회원 배지
    MEMBER_BADGE_LIST_READ("[회원]의 모든 [활동 배지]가 성공적으로 조회되었습니다.");

    private final String message;
}