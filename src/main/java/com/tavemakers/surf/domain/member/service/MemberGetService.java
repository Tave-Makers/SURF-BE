package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.exception.InvalidSignupListException;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.MemberSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberGetService {

    private final MemberRepository memberRepository;
    private final MemberSearchRepository memberSearchRepository;

    /** 회원 ID와 상태로 회원 조회 */
    public Member getMemberByStatus(Long memberId, MemberStatus memberStatus) {
        return memberRepository.findByIdAndStatus(memberId, memberStatus)
                .orElseThrow(MemberNotFoundException::new);
    }

    /** 회원 ID 목록과 상태로 회원 목록 조회 */
    public List<Member> getMembersByStatus(List<Long> memberIds, MemberStatus status) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new MemberNotFoundException();
        }

        List<Long> distinctIds = memberIds.stream().distinct().toList();
        if (distinctIds.size() != memberIds.size()) {
            throw new InvalidSignupListException();
        }

        List<Member> members = memberRepository.findAllByIdInAndStatus(distinctIds, status);
        if (members.size() != distinctIds.size()) {
            throw new MemberNotFoundException();
        }

        return members;
    }

    /** 회원 ID로 회원 조회 */
    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    /** 이메일로 회원 조회 */
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);
    }

    //회원 조회 - 이름 기반 - ID 리스트 반환
    public List<Member> getMemberByName(String name) {
        return memberRepository.findByActivityStatusAndNameAndStatusNot(true, name, MemberStatus.WITHDRAWN);
    }

    /** 회원 ID로 트랙 정보 포함 회원 조회 */
    public Member readMemberInformation(Long memberId) {
        return memberRepository.findByIdWithTracks(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    /** 회원 존재 여부 검증 */
    public Void validateMember(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        return null;
    }

    /** 기수, 파트, 키워드로 회원 검색 */
    public Slice<Member> searchMembers(Integer generation, Part part, String keyword, Pageable pageable) {
        return memberSearchRepository.searchMembers(generation, part, keyword, pageable);
    }

    /** 회원 상태로 회원 목록 검색 */
    public Slice<Member> searchMembers(MemberStatus status, Pageable pageable) {
        return memberRepository.findByMemberListStatus(status, pageable);
    }

    /** 검색 조건에 해당하는 회원 수 조회 */
    public Long countSearchingMembers(Integer generation, Part memberPart, String keyword) {
        return memberSearchRepository.countMembers(generation, memberPart, keyword);
    }

    /** 키워드와 상태 목록으로 대기 회원 검색 */
    public Slice<Member> searchWaitingMembers(String keyword, Pageable pageable, List<MemberStatus> statuses) {
        return memberSearchRepository.findWaitingMembersByName(keyword, pageable, statuses);
    }

    /** 상태 목록에 해당하는 회원 수 조회 */
    public Long countMembers(List<MemberStatus> statuses) {
        return memberSearchRepository.countMembers(statuses);
    }

    /** 회원 ID 목록으로 회원 목록 조회 (ID Set) */
    public List<Member> getMembers(Set<Long> memberIds) {
        return memberRepository.findAllById(memberIds);
    }

    /** 특정 상태가 아닌 활동 중인 회원 ID 목록 조회 */
    public List<Long> getActiveMemberIdsExcludeStatus(MemberStatus status) {
        return memberRepository.findActiveMemberIdsExcludeStatus(status);
    }

    /** 회원이 특정 상태가 아닌지 확인 */
    public boolean existsByIdAndStatusNot(Long memberId, MemberStatus status) {
        return memberRepository.existsByIdAndStatusNot(memberId, status);
    }

    public long getApprovedMemberCount() {
        return memberRepository.countByStatusAndIsDeletedFalse(MemberStatus.APPROVED);
    }

}
