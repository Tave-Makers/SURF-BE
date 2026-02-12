package com.tavemakers.surf.domain.member.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    // ADMIN
    MANAGER_PASSWORD_SET_UP_SUCCESS("[비밀번호 설정]을 완료했습니다."),
    ADMIN_PAGE_LOGIN_SUCCESS("[관리자 페이지]에 성공적으로 로그인했습니다."),
    REGISTRATION_LIST_READ("[가입신청 목록]을 조회합니다."),
    MEMBER_INFORMATION_READ("[관리자 페이지]에서 유저 정보를 조회합니다."),
    APPROVED_MEMBER_COUNT_AND_ALL_GENERATION("[전체 회원수]와 회원들의 모든 [기수]를 조회합니다."),
    APPROVED_MEMBER_LIST("승인된 [전체 회원 목록]을 조회합니다."),

    //회원가입 온보딩 확인 여부 체크
    MEMBER_ONBOARDING_STATUS_CHECK_SUCCESS("[회원]의 추가 회원가입 정보 입력 필요 여부를 확인했습니다."),

    // 이름 규칙 변경 및 메시지에 Placeholder(%s) 추가
    MEMBER_SEARCH_SUCCESS("'%s'(으)로 활동 중인 [회원]을 조회했습니다."),
    MEMBER_GROUP_SUCCESS("[트랙]별로 현재 활동 중인 [회원]을 조회했습니다."),
    MEMBER_LIST_SEARCH_SUCCESS("[회원 목록]을 검색합니다."),

    // 프로필 조회
    MYPAGE_MY_PROFILE_READ("본인 마이페이지에서 [프로필 정보]를 조회합니다."),
    MYPAGE_OTHERS_PROFILE_READ("타인 마이페이지에서 [프로필 정보]를 조회합니다."),

    // 프로필 수정
    MYPAGE_PROFILE_UPDATE_SUCCESS("마이페이지에서 [프로필 정보]를 수정했습니다."),

    // 회원 수 조회
    MEMBERS_COUNT_READ("[멤버 상태]에 따른 [회원 수]를 조회합니다."),
    ;

    private final String message;

    // String.format을 사용하여 동적인 메시지를 생성하는 헬퍼 메서드
    public String getFormattedMessage(Object... args) {
        // 메시지에 Placeholder가 없으면 그대로 반환, 있으면 값을 채워서 반환
        if (args == null || args.length == 0) {
            return this.message;
        }
        return String.format(this.message, args);
    }
}
