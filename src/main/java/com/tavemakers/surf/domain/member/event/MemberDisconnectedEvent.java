package com.tavemakers.surf.domain.member.event;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;

/**
 * 회원 연결 해제 이벤트 — 탈퇴/퇴출/제명 시 발행.
 * 외부 연결 해제(Kakao unlink / Apple revoke)와 refresh 토큰 무효화를
 * 트랜잭션 커밋 이후로 미루기 위한 이벤트다.
 *
 * <p>주의: member.withdraw()가 appleRefreshToken/kakaoId를 익명화·null 처리하므로,
 * 발행 시점의 값을 이벤트에 담아 전달한다 (엔티티를 담으면 리스너 실행 시점에 이미 값이 사라짐).
 */
public record MemberDisconnectedEvent(
        Long memberId,
        Provider provider,
        Long kakaoId,
        String appleRefreshToken
) {}
