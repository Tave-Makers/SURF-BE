package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberWithdrawService {

    private final MemberGetService memberGetService;
    private final ApplicationEventPublisher eventPublisher;

    /** 회원 탈퇴 처리 — SURF 토큰 무효화 및 provider 연결 해제는 커밋 후(AFTER_COMMIT) 수행 */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberGetService.getMember(memberId);
        expel(member);
    }

    @Transactional
    public void expel(Member member) {
        disconnectMember(member);
        if (member.getStatus() != MemberStatus.WITHDRAWN) {
            member.withdraw();
        }
    }

    /**
     * 연결 해제 이벤트 발행 — 외부 API(Kakao/Apple)와 refresh 무효화는 커밋 후 리스너가 처리.
     * member.withdraw()가 appleRefreshToken/kakaoId를 지우기 전에 값을 이벤트에 캡처한다.
     */
    public void disconnectMember(Member member) {
        eventPublisher.publishEvent(new MemberDisconnectedEvent(
                member.getId(),
                member.getProvider(),
                member.getKakaoId(),
                member.getAppleRefreshToken()
        ));
    }
}
