package com.tavemakers.surf.domain.group.service;

import com.tavemakers.surf.domain.group.dto.request.GroupCreateReqDTO;
import com.tavemakers.surf.domain.group.dto.request.GroupUpdateReqDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupDetailResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupMemberResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupListResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupResDTO;
import com.tavemakers.surf.domain.group.entity.Group;
import com.tavemakers.surf.domain.group.entity.GroupType;
import com.tavemakers.surf.domain.group.repository.GroupRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<GroupListResDTO> getGroups(Integer generation, String type) {
        GroupType filter = "ALL".equalsIgnoreCase(type) ? null : GroupType.valueOf(type);

        return groupRepository.findList(generation, filter).stream()
                .map(g -> new GroupListResDTO(
                        g.getId(),
                        g.getGeneration(),
                        g.getType(),
                        g.getName(),
                        g.getLeader().getName(),
                        g.getMemberCount()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailResDTO getGroupDetail(Long groupId) {
        Group g = groupRepository.findDetailById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        List<GroupMemberResDTO> members = g.getGroupMembers().stream()
                .map(gm -> {
                    Member m = gm.getMember();
                    // ↓ 아래 필드는 너희 Member에 맞춰 수정
                    return new GroupMemberResDTO(m.getId(), m.getTracks());
                })
                .toList();

        return new GroupDetailResDTO(
                g.getId(),
                g.getGeneration(),
                g.getType(),
                g.getName(),
                g.getLeader().getName(),
                g.getDescription(),
                members
        );
    }

    @Transactional
    public GroupResDTO createGroup(GroupCreateReqDTO req) {

        // 1) 중복 제거
        List<Long> distinctMemberIds = req.memberIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctMemberIds.size() != req.memberIds().size()) {
            throw new IllegalArgumentException("memberIds has duplicates");
        }

        // 2) 팀장은 팀원 목록에 반드시 포함
        if (!distinctMemberIds.contains(req.leaderMemberId())) {
            throw new IllegalArgumentException("leaderMemberId must be included in memberIds");
        }

        // 3) 멤버 조회
        List<Member> members = memberRepository.findAllById(distinctMemberIds);
        if (members.size() != distinctMemberIds.size()) {
            throw new IllegalArgumentException("Some members not found");
        }

        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        Member leader = memberMap.get(req.leaderMemberId());
        if (leader == null) {
            throw new IllegalArgumentException("Leader not found");
        }

        // 4) Group 생성
        Group group = Group.of(
                req.generation(),
                req.type(),
                req.name(),
                req.description(),
                leader
        );

        // leader 제외하고 추가
        for (Member m : members) {
            if (!m.getId().equals(leader.getId())) {
                group.addMember(m);
            }
        }

        Group saved = groupRepository.save(group);

        return GroupResDTO.from(saved);
    }

    @Transactional
    public void update(Long groupId, GroupUpdateReqDTO req) {
        Group group = groupRepository.findDetailById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        group.changeBasicInfo(req.name(), req.description());

        if (req.leaderMemberId() != null) {
            Member newLeader = memberRepository.findById(req.leaderMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("Leader not found"));
            group.changeLeader(newLeader);
        }

        // add
        List<Long> addIds = distinct(req.addMemberIds());
        if (!addIds.isEmpty()) {
            List<Member> addMembers = memberRepository.findAllById(addIds);
            if (addMembers.size() != addIds.size()) throw new IllegalArgumentException("Some add members not found");
            for (Member m : addMembers) group.addMember(m);
        }

        // remove
        List<Long> removeIds = distinct(req.removeMemberIds());
        for (Long memberId : removeIds) {
            group.removeMember(memberId);
        }
    }

    @Transactional
    public void delete(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group not found");
        }
        groupRepository.deleteById(groupId);
    }

    private static List<Long> distinct(List<Long> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}