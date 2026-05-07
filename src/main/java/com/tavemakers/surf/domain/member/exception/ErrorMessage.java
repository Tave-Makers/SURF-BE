package com.tavemakers.surf.domain.member.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [회원]입니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 [회원]입니다."),
    MEMBER_SIGNUP_REJECTED(HttpStatus.FORBIDDEN, "관리자에 의해 [회원가입]이 거절되었습니다."),
    INVALID_MEMBER_INFO(HttpStatus.BAD_REQUEST, "유효하지 않은 [회원 정보]입니다."),
    MEMBER_BLACKLISTED(HttpStatus.FORBIDDEN, "블랙리스트에 등록된 [회원]은 가입할 수 없습니다."),
    MEMBER_DISMISS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "[승인된 회원]만 제명할 수 있습니다."),
    TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "회원의 [트랙]이 존재하지 않습니다."),
    CAREER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않은 [경력]입니다."),
    MEMBER_STATUS_CANNOT_CONVERT(HttpStatus.BAD_REQUEST, "잘못된 [MemberStatus]입니다."),

    PASSWORD_ENCRYPTION_FAILED(HttpStatus.BAD_REQUEST ,"비밀번호 암호화에 실패했습니다."),
    PASSWORD_NOT_SETTING(HttpStatus.BAD_REQUEST, "비밀번호가 설정되지 않았습니다."),
    MIS_MATCH_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    ADMIN_PAGE_ROLE_EXCEPTION(HttpStatus.BAD_REQUEST, "관리자만 접근 가능합니다."),

    INVALID_SIGNUP_LIST(HttpStatus.BAD_REQUEST, "[회원 가입 요청 목록]이 올바르지 않습니다.")
    ;

    private final HttpStatus status;
    private final String message;

}
