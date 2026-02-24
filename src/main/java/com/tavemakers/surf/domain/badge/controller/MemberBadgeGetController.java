package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.dto.response.MemberOwnedBadgeResDTO;
import com.tavemakers.surf.domain.badge.usecase.MemberBadgeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.MEMBER_BADGE_LIST_READ;

@RestController
@RequiredArgsConstructor
@Tag(name = "배지", description = "활동 배지 관리 및 조회 관련 API")
public class MemberBadgeGetController {

    private final MemberBadgeUsecase memberBadgeUsecase;

    @Operation(summary = "해당 회원의 모든 배지 조회", description = "해당 회원이 받은 배지 모두를 조회합니다.")
    @GetMapping("/v1/user/members/{memberId}/badges")
    public ApiResponse<List<MemberOwnedBadgeResDTO>> getMemberBadges(
            @PathVariable Long memberId
    ) {

        List<MemberOwnedBadgeResDTO> response =
                memberBadgeUsecase.getAllMemberBadges(memberId);

        return ApiResponse.response(
                HttpStatus.OK,
                MEMBER_BADGE_LIST_READ.getMessage(),
                response
        );
    }
}