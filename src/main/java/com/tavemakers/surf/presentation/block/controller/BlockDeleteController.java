package com.tavemakers.surf.presentation.block.controller;

import com.tavemakers.surf.application.block.usecase.BlockUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.presentation.block.controller.ResponseMessage.BLOCK_DELETED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "차단", description = "회원 간 차단 API")
public class BlockDeleteController {

    private final BlockUsecase blockUsecase;

    /** 차단 해제 — (나 → 대상) 방향만 지운다 */
    @Operation(summary = "차단 해제",
            description = "내가 등록한 차단을 해제합니다. 상대가 나를 차단한 관계는 해제되지 않으며, "
                    + "해당 방향의 차단이 없으면 404로 응답합니다.")
    @DeleteMapping("/v1/user/blocks/{userId}")
    public ApiResponse<Void> deleteBlock(
            @Parameter(description = "차단당한 회원 ID (block_id가 아님)", example = "12")
            @PathVariable Long userId
    ) {
        Long me = SecurityUtils.getCurrentMemberId();
        blockUsecase.delete(me, userId);
        return ApiResponse.response(HttpStatus.OK, BLOCK_DELETED.getMessage());
    }
}
