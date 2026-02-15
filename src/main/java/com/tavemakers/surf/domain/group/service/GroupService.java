package com.tavemakers.surf.domain.group.service;

import com.tavemakers.surf.domain.group.dto.request.GroupUpsertReqDTO;
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

import java.util.*;
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
    public GroupResDTO createGroup(GroupUpsertReqDTO req) {

        ResolvedMembers resolved = resolveMembers(req);

        Group group = Group.of(
                req.generation(),
                req.type(),
                req.name(),
                req.description(),
                resolved.leader()
        );

        // leader 제외하고 추가
        for (Member m : resolved.members()) {
            if (!m.getId().equals(resolved.leader().getId())) {
                group.addMember(m);
            }
        }

        Group saved = groupRepository.save(group);

        return GroupResDTO.from(saved);
    }

    @Transactional
    public GroupResDTO updateGroup(Long groupId, GroupUpsertReqDTO req) {
        Group group = groupRepository.findDetailById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        ResolvedMembers resolved = resolveMembers(req);

        // 1) 기본 정보(전체) 반영
        group.changeInfo(req.generation(), req.type(), req.name(), req.description());
        group.changeLeader(resolved.leader());

        // 2) 멤버십을 요청 memberIds와 동일하게 맞추기
        // 현재 멤버 id set
        Set<Long> current = group.getGroupMembers().stream()
                .map(gm -> gm.getMember().getId())
                .collect(Collectors.toSet());

        // 요청 멤버 id set
        Set<Long> target = resolved.memberIdsSet();

        // 2-1) 추가해야 할 멤버: target - current
        for (Long memberId : target) {
            if (!current.contains(memberId)) {
                group.addMember(resolved.memberMap().get(memberId));
            }
        }

        // 2-2) 제거해야 할 멤버: current - target
        for (Long memberId : current) {
            if (!target.contains(memberId)) {
                group.removeMember(memberId);
            }
        }

        return GroupResDTO.from(group);
    }


    @Transactional
    public void delete(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group not found");
        }
        groupRepository.deleteById(groupId);
    }

    private ResolvedMembers resolveMembers(GroupUpsertReqDTO req) {
        List<Long> raw = req.memberIds();

        // 1) 중복 제거
        List<Long> distinctMemberIds = req.memberIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctMemberIds.size() != raw.size()) {
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
        if (leader == null)
            throw new IllegalArgumentException("Leader not found");

        return new ResolvedMembers(members, leader, memberMap, new HashSet<>(distinctMemberIds));
    }

    private record ResolvedMembers(
            List<Member> members,
            Member leader,
            Map<Long, Member> memberMap,
            Set<Long> memberIdsSet
    ) {}
}