package com.tavemakers.surf.presentation.block.controller;

import com.tavemakers.surf.application.block.usecase.BlockUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import com.tavemakers.surf.presentation.block.dto.request.BlockCreateReqDTO;
import com.tavemakers.surf.presentation.block.dto.response.BlockedMemberResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.presentation.block.controller.ResponseMessage.BLOCK_CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "차단", description = "회원 간 차단 API")
public class BlockCreateController {

    private final BlockUsecase blockUsecase;

    /** 차단 등록 (현재 로그인 사용자 기준) */
    @Operation(summary = "차단 등록",
            description = "특정 회원을 차단합니다. 자기 자신은 400, 없거나 탈퇴한 회원은 404, "
                    + "이미 차단한 회원은 409로 응답합니다. 차단 상대에게는 알림이 가지 않습니다.")
    @PostMapping("/v1/user/blocks")
    public ApiResponse<BlockedMemberResDTO> createBlock(
            @RequestBody @Valid BlockCreateReqDTO request
    ) {
        Long me = SecurityUtils.getCurrentMemberId();
        BlockedMemberResDTO response = blockUsecase.create(me, request.memberId());
        return ApiResponse.response(HttpStatus.CREATED, BLOCK_CREATED.getMessage(), response);
    }
}
