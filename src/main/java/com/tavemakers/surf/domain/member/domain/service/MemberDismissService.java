package com.tavemakers.surf.domain.member.domain.service;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.domain.exception.MemberDismissNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberDismissService {

    /** 제명 가능 상태인지 검증 — APPROVED 회원만 제명 가능 */
    public void validateDismissible(Member member) {
        if (member.getStatus() != MemberStatus.APPROVED) {
            throw new MemberDismissNotAllowedException();
        }
    }
}
