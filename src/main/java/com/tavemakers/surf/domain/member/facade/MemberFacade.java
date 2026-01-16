package com.tavemakers.surf.domain.member.facade;

import com.tavemakers.surf.domain.member.dto.request.MemberSignupReqDTO;
import com.tavemakers.surf.domain.member.dto.request.ProfileUpdateReqDTO;
import com.tavemakers.surf.domain.member.dto.response.*;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.exception.TrackNotFoundException;
import com.tavemakers.surf.domain.member.service.*;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberFacade {

    private final MemberGetService memberGetService;
    private final TrackGetService trackGetService;
    private final PersonalScoreGetService personalScoreGetService;
    private final CareerPostService careerPostService;
    private final CareerPatchService careerPatchService;
    private final CareerDeleteService careerDeleteService;
    private final CareerGetService careerGetService;
    private final MemberPatchService memberPatchService;
    private final MemberService memberService;
    private final ApplicationContext context;

    public MyPageProfileResDTO getMyPageAndProfile(Long targetId) {
        Member member = memberGetService.getMemberByStatus(targetId,MemberStatus.APPROVED);
        List<TrackResDTO> myTracks = getMyTracks(targetId);
        List<CareerResDTO> myCareers = getMyCareers(targetId);

        if (member.isNotOwner()) {
            return MyPageProfileResDTO.of(member, myTracks, null, myCareers);
        }

        BigDecimal score = null;
        if (member.isActive()) {
            score = personalScoreGetService.getPersonalScore(targetId).getScore();
        }

        return MyPageProfileResDTO.of(member, myTracks, score, myCareers);
    }

    public List<MemberSearchResDTO> findMemberByNameAndTrack(String name) {
        List<Member> members = memberGetService.getMemberByName(name);
        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = members.stream().map(Member::getId).collect(Collectors.toList());

        List<Track> latestTracks = trackGetService.getTrack(memberIds);

        Map<Long, Track> trackMap = latestTracks.stream()
                .collect(Collectors.toMap(track -> track.getMember().getId(), track -> track));

        List<MemberSearchResDTO> result = new ArrayList<>();
        for (Member member : members) {
            Track latestTrack = trackMap.get(member.getId());

            if (latestTrack == null) {
                throw new TrackNotFoundException ("해당 회원의 트랙을 찾을 수 없습니다. 이름/id : " + member.getName() + "/" +member.getId());
            }

            result.add(MemberSearchResDTO.of(member, latestTrack.getGeneration(), latestTrack.getPart().toString()));
        }

        return result;
    }

    public Map<String, List<MemberSimpleResDTO>> getMembersGroupedByTrack() {
        return trackGetService.getAllTracksWithMember().stream()
                .collect(Collectors.groupingBy(
                        track -> track.getPart().name() + "_" + track.getGeneration() + "기",
                        Collectors.mapping(
                                track -> MemberSimpleResDTO.from(track.getMember()),
                                Collectors.toList()
                        )
                ));
    }

    @LogEvent(value = "member.profile_update", message = "회원 정보 수정")
    @Transactional
    public void updateProfile(@LogParam("member_id") Long memberId,
                              ProfileUpdateReqDTO dto) {

        Member member = memberGetService.getMember(memberId);

        memberPatchService.updateProfile(member, dto);

        if (dto.careersToUpdate() != null) {
            careerPatchService.updateCareer(member, dto.careersToUpdate());
        }

        if (dto.careerIdsToDelete() != null) {
            careerDeleteService.deleteCareer(member, dto.careerIdsToDelete());
        }

        if (dto.careersToCreate() != null) {
            careerPostService.createCareer(member, dto.careersToCreate());
        }
    }

    @Transactional(readOnly = true)
    public OnboardingCheckResDTO needsOnboarding(
            Long memberId
    ) {
        Member member = memberGetService.getMember(memberId);

        Boolean needOnboarding = memberService.needsOnboarding(member);
        MemberStatus memberStatus = memberService.memberStatusCheck(member);

        MemberRole memberRole = SecurityUtils.getCurrentMember().getRole();

        OnboardingCheckResDTO dto = OnboardingCheckResDTO.of(memberId, needOnboarding, memberStatus, memberRole);
        return dto;
    }

    @Transactional
    public MemberSignupResDTO signup(
            Long memberId,
            MemberSignupReqDTO request
    ) {
        Member member = memberGetService.getMember(memberId);
        MemberStatus status = member.getStatus();

        MemberFacade proxy = context.getBean(MemberFacade.class);

        if (status == MemberStatus.APPROVED) {
            MemberSignupResDTO dto = MemberSignupResDTO.from(member);
            return proxy.signupSucceeded(memberId, dto);
        }

        if (status == MemberStatus.REJECTED) {
            int statusCode = 403;
            String errorReason = "ADMIN_REJECTED";

            try {
                proxy.signupFailed(memberId, statusCode, errorReason);
            } catch (RuntimeException ignored) {}
            return MemberSignupResDTO.from(member);
        }

        return proxy.signupCreate(member, request);

    }

    @Transactional
    @LogEvent(value = "signup.create", message = "회원가입 요청 처리")
    public MemberSignupResDTO signupCreate(Member member, MemberSignupReqDTO request) {
        return memberService.signup(member, request);
    }

    @Transactional
    @LogEvent(value = "signup.succeeded", message = "회원가입 성공")
    public MemberSignupResDTO signupSucceeded(
            @LogParam("member_id") Long memberId,
            MemberSignupResDTO response
    ) {
        return response;
    }

    @Transactional
    @LogEvent(value = "signup.failed", message = "회원가입 실패")
    public MemberSignupResDTO signupFailed(
            Long memberId,
            int statusCode,
            String errorReason
    ) {
        throw new RuntimeException(errorReason);
    }

    private List<CareerResDTO> getMyCareers(Long memberId) {
        return careerGetService.getMyCareers(memberId)
                .stream().map(CareerResDTO::from).toList();
    }

    private List<TrackResDTO> getMyTracks(Long memberId) {
        return trackGetService.getTrackSortedByGeneration(memberId)
                .stream()
                .map(TrackResDTO::from)
                .toList();
    }

    public MemberSearchSliceResDTO searchMembers( int pageNum, int pageSize, Integer generation, String part, String keyword) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Part memberPart = part == null ? null : Part.valueOf(part);

        Slice<MemberSearchDetailResDTO> slice = search(generation, memberPart, keyword, pageable);

        Long totalCount = null;
        if (pageNum == 0) {
            totalCount = memberGetService.countSearchingMembers(generation, memberPart, keyword);
        }

        return MemberSearchSliceResDTO.of(slice, totalCount);
    }

    private Slice<MemberSearchDetailResDTO> search(Integer generation, Part part, String keyword, Pageable pageable) {
        return memberGetService.searchMembers(generation, part, keyword, pageable)
                .map(MemberSearchDetailResDTO::from);
    }

}
