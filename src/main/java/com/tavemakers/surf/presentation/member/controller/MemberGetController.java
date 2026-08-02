package com.tavemakers.surf.presentation.member.controller;

import com.tavemakers.surf.application.member.usecase.MemberUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import com.tavemakers.surf.presentation.member.dto.response.GenerationInfoListResDTO;
import com.tavemakers.surf.presentation.member.dto.response.MemberGroupedByPartResDTO;
import com.tavemakers.surf.presentation.member.dto.response.MemberSearchResDTO;
import com.tavemakers.surf.presentation.member.dto.response.MemberSearchSliceResDTO;
import com.tavemakers.surf.presentation.member.dto.response.MemberSimpleResDTO;
import com.tavemakers.surf.presentation.member.dto.response.MembersCountByMemberStatusResDTO;
import com.tavemakers.surf.presentation.member.dto.response.MyPageProfileResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.tavemakers.surf.presentation.member.controller.ResponseMessage.APPROVED_ALL_GENERATION;
import static com.tavemakers.surf.presentation.member.controller.ResponseMessage.MEMBER_GROUP_SUCCESS;
import static com.tavemakers.surf.presentation.member.controller.ResponseMessage.MEMBER_LIST_SEARCH_SUCCESS;
import static com.tavemakers.surf.presentation.member.controller.ResponseMessage.MEMBERS_COUNT_READ;
import static com.tavemakers.surf.presentation.member.controller.ResponseMessage.MYPAGE_MY_PROFILE_READ;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "회원 조회", description = "회원 조회 관련 API")
public class MemberGetController {

    private final MemberUsecase memberUsecase;

    @Operation(
            summary = "이름 기반 회원 조회",
            description = "특정 이름을 가진 회원 목록을 조회합니다.")
    @GetMapping("/v1/admin/members/search")
    public ApiResponse<List<MemberSearchResDTO>> searchMemberByName(
            @RequestParam @NotBlank(message = "검색어(name)는 필수입니다.") String name
    ) {
        return ApiResponse.response(
                HttpStatus.OK,
                MEMBER_GROUP_SUCCESS.getFormattedMessage(name),
                memberUsecase.findMemberByNameAndTrack(name)
        );
    }

    @Operation(
            summary = "활동 회원 트랙별 조회",
            description = "활동 중인 회원을 트랙과 기수 기준으로 그룹화해 조회합니다.")
    @GetMapping("/v1/admin/members/search/grouped-by-track")
    public ApiResponse<Map<String, List<MemberSimpleResDTO>>> getGroupedMembers() {
        return ApiResponse.response(
                HttpStatus.OK,
                MEMBER_GROUP_SUCCESS.getMessage(),
                memberUsecase.getMembersGroupedByTrack()
        );
    }

    @Operation(
            summary = "활동 기수 기준 파트별 멤버 조회",
            description = "활동 기수에 해당하는 활동 멤버를 파트별로 묶어 조회합니다.")
    @GetMapping("/v1/admin/members/grouped-by-part")
    public ApiResponse<List<MemberGroupedByPartResDTO>> getGroupedMembersByPart(
            @RequestParam Integer generation
    ) {
        return ApiResponse.response(
                HttpStatus.OK,
                MEMBER_GROUP_SUCCESS.getMessage(),
                memberUsecase.getMembersGroupedByPart(generation)
        );
    }

    @Operation(
            summary = "마이페이지 프로필 조회",
            description = "마이페이지에서 프로필 정보를 조회합니다.")
    @GetMapping("/v1/user/members/profile")
    public ApiResponse<MyPageProfileResDTO> getMyPageAndProfile(
            @RequestParam(required = false) Long memberId
    ) {
        memberId = (memberId == null ? SecurityUtils.getCurrentMemberId() : memberId);
        MyPageProfileResDTO response = memberUsecase.getMyPageAndProfile(memberId);
        return ApiResponse.response(HttpStatus.OK, MYPAGE_MY_PROFILE_READ.getMessage(), response);
    }

    @Operation(
            summary = "회원이름 및 학교로 검색 (기수/파트 필터링)",
            description = "회원이름 및 학교로 회원을 검색합니다. (기수/파트 필터링)"
    )
    @GetMapping("/v1/user/members")
    public ApiResponse<MemberSearchSliceResDTO> searchMembers(
            @RequestParam int pageNum,
            @RequestParam int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String part
    ) {
        MemberSearchSliceResDTO response = memberUsecase.searchMembers(pageNum, pageSize, generation, part, keyword);
        return ApiResponse.response(HttpStatus.OK, MEMBER_LIST_SEARCH_SUCCESS.getMessage(), response);
    }

    @Operation(
            summary = "회원 수 조회",
            description = "회원 상태 목록과 키워드 기준으로 회원 수를 조회합니다.")
    @GetMapping("/v1/user/members-count")
    public ApiResponse<MembersCountByMemberStatusResDTO> getMembersCount(
            @RequestParam List<String> memberStatuses,
            @RequestParam(required = false) String keyword
    ) {
        MembersCountByMemberStatusResDTO data = memberUsecase.getMembersCountByMemberStatusAndKeyword(memberStatuses, keyword);
        return ApiResponse.response(HttpStatus.OK, MEMBERS_COUNT_READ.getMessage(), data);
    }

    @Operation(
            summary = "전체 기수 조회",
            description = "존재하는 모든 기수 정보를 조회합니다.")
    @GetMapping("/v1/user/generations")
    public ApiResponse<GenerationInfoListResDTO> readAllMemberCountAndGeneration() {
        GenerationInfoListResDTO data = memberUsecase.readExistingGenerations();
        return ApiResponse.response(HttpStatus.OK, APPROVED_ALL_GENERATION.getMessage(), data);
    }
}
