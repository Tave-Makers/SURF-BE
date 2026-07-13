package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberPatchService {

    private final MemberRepository memberRepository;

    /** 회원 프로필 정보 수정 */
    public void updateProfile(Member member,
                              String email,
                              String university,
                              String graduateSchool,
                              String selfIntroduction,
                              String link,
                              String phoneNumber,
                              Boolean phoneNumberPublic,
                              String profileImageUrl,
                              Boolean isProfileImageChanged) {
        member.updateProfile(email, university, graduateSchool, selfIntroduction, link,
                phoneNumber, phoneNumberPublic, profileImageUrl, isProfileImageChanged);
    }

    public void grantRole(Member member, MemberRole role) {
        //유저 권한 부여
        member.exchangeRole(role);
    }

    /** 약관 동의 처리 */
    public void agreeTerms(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        member.agreeTerms();
    }

    /** 여러 회원의 권한을 일괄 변경 version 2*/
    public void grantRoleV2(List<Member> members, MemberRole role) {
        members.forEach(member -> member.exchangeRole(role));

    }
}
