package com.tavemakers.surf.domain.member.usecase;

import com.tavemakers.surf.domain.member.dto.request.MemberSignupReqDTO;
import com.tavemakers.surf.domain.member.dto.request.ProfileUpdateReqDTO;
import com.tavemakers.surf.domain.member.dto.response.*;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.exception.MemberAlreadyExistsException;
import com.tavemakers.surf.domain.member.exception.MemberSignupRejectedException;
import com.tavemakers.surf.domain.member.exception.TrackNotFoundException;
import com.tavemakers.surf.domain.member.service.*;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tavemakers.surf.global.logging.LogEventEmitter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberUsecase {

    //<editor-fold desc="MemberUsecase Dependency Summary">
    private final MemberGetService memberGetService;
    private final TrackGetService trackGetService;
    private final PersonalScoreGetService personalScoreGetService;
    private final CareerCreateService careerCreateService;
    private final CareerPatchService careerPatchService;
    private final CareerDeleteService careerDeleteService;
    private final CareerGetService careerGetService;
    private final MemberPatchService memberPatchService;
    private final MemberService memberService;
    private final MemberWithdrawService memberWithdrawService;
    private final ApplicationContext context;
    private final LogEventEmitter logEventEmitter;
    //</editor-fold>

    /** 마이페이지 + 프로필 조회 */
    public MyPageProfileResDTO getMyPageAndProfile(Long targetId) {

        Long requesterId = SecurityUtils.getCurrentMemberId();

        logEventEmitter.emit(
                "member_profile_api_called",
                Map.of(
                        "requester_id", requesterId,
                        "target_member_id", targetId
                )
        );

        try {

            Member member =
                    memberGetService.getMemberByStatus(
                            targetId,
                            MemberStatus.APPROVED
                    );

            List<TrackResDTO> myTracks = getMyTracks(targetId);
            List<CareerResDTO> myCareers = getMyCareers(targetId);

            MyPageProfileResDTO result;

            if (member.isNotOwner()) { // SURF Rule - 타인의 활동점수는 조회 불가

                result = MyPageProfileResDTO.of(
                        member,
                        myTracks,
                        null,
                        myCareers
                );

            } else {

                BigDecimal score = null;

                if (member.isActive()) { // SURF Rule - 활동 중인 회원만 활동점수를 보여준다.
                    score = personalScoreGetService
                            .getPersonalScore(targetId)
                            .getScore();
                }

                result = MyPageProfileResDTO.of(
                        member,
                        myTracks,
                        score,
                        myCareers
                );
            }

            logEventEmitter.emit(
                    "member_profile_api_succeeded",
                    Map.of(
                            "requester_id", requesterId,
                            "target_member_id", targetId
                    )
            );

            return result;

        } catch (Exception e) {

            Map<String, Object> failedProps = new HashMap<>();

            failedProps.put("requester_id", requesterId);
            failedProps.put("target_member_id", targetId);
            failedProps.put("status_code", 500);
            failedProps.put("error_code", e.getClass().getSimpleName());

            logEventEmitter.emitError(
                    "member_profile_api_failed",
                    failedProps,
                    "회원 프로필 조회 실패"
            );

            throw e;
        }
    }


    /** 이름으로 회원 검색 후 각 회원의 트랙 정보를 DTO로 반환하는 메소드 **/
    public List<MemberSearchResDTO> findMemberByNameAndTrack(String name) {
        List<Member> members = memberGetService.getMemberByName(name);
        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = members.stream().map(Member::getId).collect(Collectors.toList());

        List<Track> latestTracks = trackGetService.getTrack(memberIds);

        // 조회된 최신 트랙들을 Member ID를 Key로 하는 Map으로 변환
        Map<Long, Track> trackMap = latestTracks.stream()
                .collect(Collectors.toMap(track -> track.getMember().getId(), track -> track));

        List<MemberSearchResDTO> result = new ArrayList<>();
        for (Member member : members) {
            Track latestTrack = trackMap.get(member.getId());

            if (latestTrack == null) {
                throw new TrackNotFoundException ("해당 회원의 트랙을 찾을 수 없습니다. 이름/id : " + member.getName() + "/" +member.getId());
            }

            //현재는 기수까지 불러오고 있는데 추후에 기수가 필요하지 않다면 삭제 가능
            result.add(MemberSearchResDTO.of(member, latestTrack.getGeneration(), latestTrack.getPart().toString()));
        }

        return result;
    }

    /** 트랙+기수별 회원 묶기 */
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

    /** 회원 프로필 및 경력 정보 수정 */
    @Transactional
    public void updateProfile(Long memberId,
                              ProfileUpdateReqDTO dto) {

        try {

            Member member = memberGetService.getMember(memberId);

            // 프로필 정보 수정
            memberPatchService.updateProfile(member, dto);

            // 경력 수정
            if (dto.careersToUpdate() != null) {
                careerPatchService.updateCareer(member, dto.careersToUpdate());
            }

            // 경력 삭제
            if (dto.careerIdsToDelete() != null) {
                careerDeleteService.deleteCareer(member, dto.careerIdsToDelete());
            }

            // 경력 생성
            if (dto.careersToCreate() != null) {
                careerCreateService.createCareer(member, dto.careersToCreate());
            }

            Map<String, Object> props = new HashMap<>(dto.buildProps());

            props.put("member_id", memberId);

            List<?> changedFields =
                    (List<?>) props.getOrDefault("changed_fields", List.of());

            props.put(
                    "updated_fields_count",
                    changedFields.size()
            );

            logEventEmitter.emit(
                    "profile.update.succeeded",
                    props
            );

        } catch (RuntimeException e) {

            int errorCode = 500; // 기본값
            logEventEmitter.emitError(
                    "profile.update.failed",
                    Map.of(
                            "member_id", memberId,
                            "error_code", errorCode,
                            "error_msg", e.getClass().getSimpleName()
                    ),
                    "회원 프로필 수정 실패"
            );

            throw e;
        }
    }

    /** 온보딩 필요 여부 확인 */
    @Transactional(readOnly = true)
    public OnboardingCheckResDTO needsOnboarding(
            Long memberId
    ) {
        Member member = memberGetService.getMember(memberId);

        Boolean needOnboarding = member.isRegistering();
        MemberStatus memberStatus = member.getStatus();

        MemberRole memberRole = SecurityUtils.getCurrentMember().getRole();

        logEventEmitter.emit("onboarding.valid_status", Map.of(
                "need_onboarding", needOnboarding
        ));

        return OnboardingCheckResDTO.of(memberId, needOnboarding, memberStatus, memberRole);
    }

    /** 회원가입 요청 및 MemberStatus에 따른 로그 분기 */
    @Transactional
    public MemberSignupResDTO signup(
            Long memberId,
            MemberSignupReqDTO request
    ) {
        Member member = memberGetService.getMember(memberId);
        MemberStatus status = member.getStatus();

        MemberUsecase proxy = context.getBean(MemberUsecase.class);

        if (status == MemberStatus.APPROVED) {
            throw new MemberAlreadyExistsException();
        }

        if (status == MemberStatus.REJECTED) {
            throw new MemberSignupRejectedException();
        }

        return proxy.signupCreate(member, request);

    }

    /** 회원가입 create 로그 (온보딩) */
    @Transactional
    @LogEvent(value = "signup.create", message = "회원가입 요청 처리")
    public MemberSignupResDTO signupCreate(Member member, MemberSignupReqDTO request) {
        return memberService.signup(member, request);
    }

    /** 회원 탈퇴 처리 */
    @Transactional
    public void withdraw(Long memberId) {
        memberWithdrawService.withdraw(memberId);
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

    /** 조건별 회원 검색 및 페이징 처리 */
    public MemberSearchSliceResDTO searchMembers(int pageNum, int pageSize, Integer generation, String part, String keyword) {
        Long requesterId = SecurityUtils.getCurrentMemberId();

        logEventEmitter.emit("member_list_api_called", Map.of("requester_id", requesterId));
        if (keyword != null && !keyword.isBlank()) {
            logEventEmitter.emit("member_search_api_called", Map.of(
                    "requester_id", requesterId,
                    "keyword_length", keyword.length()
            ));
        }
        if (generation != null || part != null) {
            String filterType = generation != null ? "generation" : "part";
            Object filterValue = generation != null ? (Object) generation : part;
            logEventEmitter.emit("member_filter_api_called", Map.of(
                    "requester_id", requesterId,
                    "filter_type", filterType,
                    "filter_value", filterValue
            ));
        }

        try {
            Pageable pageable = PageRequest.of(pageNum, pageSize);
            Part memberPart = part == null ? null : Part.valueOf(part);
            Slice<MemberSearchDetailResDTO> slice = search(generation, memberPart, keyword, pageable);

            Long totalCount = null;
            if (pageNum == 0) { // FRONTEND 협의 - 0번째 페이지에서만 검색조건에 따른 전체 회원수 조회.
                totalCount = memberGetService.countSearchingMembers(generation, memberPart, keyword);
            }

            MemberSearchSliceResDTO result = MemberSearchSliceResDTO.of(slice, totalCount);

            Map<String, Object> succeededProps = new HashMap<>();
            succeededProps.put("requester_id", requesterId);
            succeededProps.put("page_num", pageNum);
            succeededProps.put("page_size", pageSize);
            succeededProps.put("total_count", totalCount);
            succeededProps.put("returned_count", result.numberOfElements());
            logEventEmitter.emit("member_list_api_succeeded", succeededProps);

            if (keyword != null && !keyword.isBlank()) {
                logEventEmitter.emit("member_search_api_succeeded", Map.of(
                        "requester_id", requesterId,
                        "keyword", keyword,
                        "result_count", result.numberOfElements()
                ));
            }
            if (generation != null || part != null) {
                String filterType = generation != null ? "generation" : "part";
                Object filterValue = generation != null ? (Object) generation : part;
                logEventEmitter.emit("member_filter_api_succeeded", Map.of(
                        "requester_id", requesterId,
                        "filter_type", filterType,
                        "filter_value", filterValue,
                        "result_count", result.numberOfElements()
                ));
            }

            return result;
        } catch (Exception e) {
            Map<String, Object> failedProps = new HashMap<>();
            failedProps.put("requester_id", requesterId);
            failedProps.put("status_code", 500);
            failedProps.put("error_code", e.getClass().getSimpleName());
            failedProps.put("error_message", e.getMessage());
            logEventEmitter.emitError("member_list_api_failed", failedProps, "회원 목록 조회 실패");
            throw e;
        }
    }

    /** MemberStatus에 따른 총 회원 수 카운트 조회 */
    public MembersCountByMemberStatusResDTO getMembersCountByMemberStatusAndKeyword(List<String> rawMemberStatuses, String keyword) {
        List<MemberStatus> memberStatuses = MemberStatus.toList(rawMemberStatuses);
        long membersCount = memberGetService.countMembers(memberStatuses, keyword);
        return MembersCountByMemberStatusResDTO.of(memberStatuses, membersCount);
    }

    /** 존재하는 모든 기수를 구함. */
    public GenerationInfoListResDTO readExistingGenerations() {
        List<Integer> existsAllGenerations = trackGetService.getExistsAllGenerations();
        return GenerationInfoListResDTO.from(existsAllGenerations);
    }


    private Slice<MemberSearchDetailResDTO> search(Integer generation, Part part, String keyword, Pageable pageable) {
        return memberGetService.searchMembers(generation, part, keyword, pageable)
                .map(MemberSearchDetailResDTO::from);
    }

}
