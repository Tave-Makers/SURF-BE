package com.tavemakers.surf.domain.group.service;

import com.tavemakers.surf.domain.group.dto.request.GroupUpsertReqDTO;
import com.tavemakers.surf.domain.group.dto.response.*;
import com.tavemakers.surf.domain.group.entity.Group;
import com.tavemakers.surf.domain.group.entity.GroupMember;
import com.tavemakers.surf.domain.group.entity.GroupType;
import com.tavemakers.surf.domain.group.repository.GroupRepository;
import com.tavemakers.surf.domain.member.dto.response.TrackResDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
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
    private final TrackRepository trackRepository;

    @Transactional(readOnly = true)
    public List<GroupGenerationSectionResDTO> getGroups(String type) {
        GroupType filterType = null;
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            filterType = GroupType.valueOf(type);
        }

        List<GroupListResDTO> flat = groupRepository.findAllForAccordion(filterType).stream()
                .map(GroupListResDTO::from)
                .toList();

        Map<Integer, List<GroupListResDTO>> grouped = new LinkedHashMap<>();
        for (GroupListResDTO dto : flat) {
            grouped.computeIfAbsent(dto.generation(), k -> new ArrayList<>()).add(dto);
        }

        return grouped.entrySet().stream()
                .map(e -> new GroupGenerationSectionResDTO(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailResDTO getGroupDetail(Long groupId) {
        Group g = groupRepository.findDetailBaseById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // 1) tracks 조회에 필요한 memberIds
        Set<Long> memberIdSet = new HashSet<>();
        memberIdSet.add(g.getLeader().getId());
        g.getGroupMembers().forEach(gm -> memberIdSet.add(gm.getMember().getId()));
        List<Long> memberIds = memberIdSet.stream().toList();

        // 2) Track을 한 번에 조회 (N+1 방지)
        Map<Long, List<Track>> trackMap = trackRepository.findAllByMemberIds(memberIds).stream()
                .collect(Collectors.groupingBy(t -> t.getMember().getId()));

        // 3) 팀장 DTO
        GroupDetailResDTO.MemberCardDTO leaderDto = toMemberCard(g.getLeader(), trackMap);

        // 4) members: 리더 제외 정렬(최신 기수 순 -> 이름 순)
        List<Member> members = g.getGroupMembers().stream()
                .map(GroupMember::getMember)
                .filter(m -> !m.getId().equals(g.getLeader().getId()))
                .sorted(memberComparator(trackMap))
                .toList();

        List<GroupDetailResDTO.MemberCardDTO> memberDtos = members.stream()
                .map(m -> toMemberCard(m, trackMap))
                .toList();

        return GroupDetailResDTO.from(g, leaderDto, memberDtos);
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
        Group group = groupRepository.findDetailBaseById(groupId)
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

    private GroupDetailResDTO.MemberCardDTO toMemberCard(Member m, Map<Long, List<Track>> trackMap) {
        List<TrackResDTO> tracks = trackMap.getOrDefault(m.getId(), List.of()).stream()
                .map(TrackResDTO::from)
                .toList();

        return new GroupDetailResDTO.MemberCardDTO(
                m.getId(),
                m.getName(),
                m.getProfileImageUrl(),
                tracks
        );
    }

    private Comparator<Member> memberComparator(Map<Long, List<Track>> trackMap) {
        Comparator<Integer> generationDesc = Comparator.nullsLast(Comparator.reverseOrder());

        return Comparator
                .comparing((Member m) -> mainGeneration(m.getId(), trackMap), generationDesc)
                .thenComparing(Member::getName, Comparator.nullsLast(String::compareTo));
    }

    private Integer mainGeneration(Long memberId, Map<Long, List<Track>> trackMap) {
        return trackMap.getOrDefault(memberId, List.of()).stream()
                .map(Track::getGeneration)
                .max(Integer::compareTo)
                .orElse(null);
    }
}