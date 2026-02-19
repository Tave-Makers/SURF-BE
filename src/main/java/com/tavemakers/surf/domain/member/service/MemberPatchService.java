package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.dto.request.ProfileUpdateReqDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.AlreadyBannedMemberException;
import com.tavemakers.surf.domain.member.exception.CanBanApprovedMemberException;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberPatchService {

    private final MemberRepository memberRepository;

    /** 회원 프로필 정보 수정 */
    @Transactional
    public void updateProfile(Member member, ProfileUpdateReqDTO dto) {
        member.updateProfile(dto);
    }

    @Transactional
    public void grantRole(Member member, MemberRole role) {
        //유저 권한 부여
        member.exchangeRole(role);
    }

    /** 여러 회원의 권한을 일괄 변경 version 2*/
    @Transactional
    public void grantRoleV2(List<Member> members, MemberRole role) {
        members.forEach(member -> member.exchangeRole(role));
    }

    @Transactional
    public void banMembers(List<Long> memberIds) {

        List<Member> members = memberRepository.findAllById(memberIds);

        if (members.size() != memberIds.size()) {
            throw new MemberNotFoundException();
        }

        boolean alreadyBannedExists = members.stream()
                .anyMatch(Member::isBanned);

        if (alreadyBannedExists) {
            throw new AlreadyBannedMemberException();
        }

        boolean notApprovedExists = members.stream()
                .anyMatch(m -> m.getStatus() != MemberStatus.APPROVED);

        if (notApprovedExists) {
            throw new CanBanApprovedMemberException();
        }

        members.forEach(Member::ban);
    }
}
