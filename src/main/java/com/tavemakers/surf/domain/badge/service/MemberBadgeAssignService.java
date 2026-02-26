package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.exception.MemberBadgeAlreadyExistsException;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import com.tavemakers.surf.domain.badge.repository.MemberBadgeRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberBadgeAssignService {

    private final BadgeRepository badgeRepository;
    private final MemberRepository memberRepository;
    private final MemberBadgeRepository memberBadgeRepository;

    /** 다수의 회원에게 배지 부여 */
    @Transactional
    public void assign(Long badgeId, List<Long> memberIds) {

        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);

        // 회원 한 번에 조회
        List<Member> members = memberRepository.findAllById(memberIds);

        if (members.size() != memberIds.size()) {
            throw new MemberNotFoundException();
        }

        // 해당 배지가 이미 존재하는 회원 체크 (중복 적용 방지)
        List<MemberBadge> existing =
                memberBadgeRepository.findByBadgeIdAndMemberIdIn(badgeId, memberIds);

        if (!existing.isEmpty()) {
            throw new MemberBadgeAlreadyExistsException();
        }

        // bulk 생성
        List<MemberBadge> newMemberBadges = members.stream()
                .map(member -> MemberBadge.create(member, badge))
                .toList();

        memberBadgeRepository.saveAll(newMemberBadges);
    }
}