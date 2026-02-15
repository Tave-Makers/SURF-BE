package com.tavemakers.surf.domain.group.service;

import com.tavemakers.surf.domain.group.dto.request.GroupCreateReqDTO;
import com.tavemakers.surf.domain.group.dto.request.GroupUpdateReqDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupDetailResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupMemberResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupListResDTO;
import com.tavemakers.surf.domain.group.entity.Group;
import com.tavemakers.surf.domain.group.entity.GroupType;
import com.tavemakers.surf.domain.group.repository.GroupRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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
    public Long create(GroupCreateReqDTO req) {
        Member leader = memberRepository.findById(req.leaderMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Leader not found"));

        Group group = Group.of(
                req.generation(),
                req.type(),
                req.name(),
                req.description(),
                leader
        );

        // 멤버 추가: Group AR 메서드를 통해서만
        List<Long> ids = distinct(req.memberIds());
        if (!ids.isEmpty()) {
            List<Member> members = memberRepository.findAllById(ids);
            if (members.size() != ids.size()) throw new IllegalArgumentException("Some members not found");

            for (Member m : members) {
                // 리더는 of()에서 이미 포함되어 있으므로 addMember에서 중복이면 예외가 날 수 있음
                if (!m.getId().equals(leader.getId())) {
                    group.addMember(m);
                }
            }
        }

        return groupRepository.save(group).getId();
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