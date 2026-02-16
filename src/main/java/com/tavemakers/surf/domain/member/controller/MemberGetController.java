package com.tavemakers.surf.domain.member.controller;

import com.tavemakers.surf.domain.member.dto.response.*;
import com.tavemakers.surf.domain.member.usecase.MemberUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.tavemakers.surf.domain.member.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "회원 조회", description = "회원 조회 관련 API")
public class MemberGetController {

    private final MemberUsecase memberUsecase;

    //이름 기반 조회
    @Operation(
            summary = "이름 기반 회원 조회",
            description = "파라미터로 특정 이름을 받아 해당하는 회원 리스트를 반환합니다.")
    @GetMapping("/v1/admin/members/search")
    public ApiResponse<List<MemberSearchResDTO>> searchMemberByName(
            @RequestParam @NotBlank(message = "검색어(name)은 필수입니다.") String name) {

        return ApiResponse.response(
                HttpStatus.OK,
                ResponseMessage.MEMBER_GROUP_SUCCESS.getFormattedMessage(name),
                memberUsecase.findMemberByNameAndTrack(name));
    }

    //유저 전체 출력시 트랙+기수별 묶어서 출력
    @Operation(
            summary = "활동 중인 회원 전체 출력시 트랙+기수별로 출력 ",
            description = "활동 중인 회원 전체 출력시 트랙+기수별로 출력")
    @GetMapping("/v1/admin/members/search/grouped-by-track")
    public ApiResponse<Map<String, List<MemberSimpleResDTO>>> getGroupedMembers() {
        return ApiResponse.response(
                HttpStatus.OK,
                ResponseMessage.MEMBER_GROUP_SUCCESS.getMessage(),
                memberUsecase.getMembersGroupedByTrack());
    }

    @Operation(
            summary = "마이페이지에서 프로필 정보 조회",
            description = "마이페이지에서 프로필 정보 조회")
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
            description = "회원이름 및 학교로 검색 (기수/파트 필터링)"
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
            summary = "멤버 상태에 따른 전체 회원 수 조회",
            description = "멤버 상태에 따른 전체 회원 수 조회"
    )
    @GetMapping("/v1/user/members-count")
    public ApiResponse<MembersCountByMemberStatusResDTO> getMembersCount(
            @RequestParam List<String> memberStatuses,
            @RequestParam(required = false) String keyword
    ) {
        MembersCountByMemberStatusResDTO data = memberUsecase.getMembersCountByMemberStatusAndKeyword(memberStatuses, keyword);
        return ApiResponse.response(HttpStatus.OK, MEMBERS_COUNT_READ.getMessage(), data);
    }

}
