package com.tavemakers.surf.presentation.block.controller;

import com.tavemakers.surf.application.block.usecase.BlockUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import com.tavemakers.surf.presentation.block.dto.response.BlockSliceResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.presentation.block.controller.ResponseMessage.MY_BLOCK_LIST_READ;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "차단", description = "회원 간 차단 API")
public class BlockGetController {

    private final BlockUsecase blockUsecase;

    /**
     * 내 차단 목록 (최신순).
     *
     * <p>정렬은 서버가 {@code createdAt DESC, id DESC}로 고정한다. 클라이언트 정렬 파라미터를 받으면
     * Slice 커서가 흔들리므로 {@code Pageable}의 sort는 쓰지 않는다.
     */
    @Operation(summary = "내 차단 목록",
            description = "내가 차단한 회원 목록을 최신순으로 조회합니다. "
                    + "나를 차단한 회원은 노출되지 않으며, 비어 있으면 content는 빈 배열입니다.")
    @GetMapping("/v1/user/blocks")
    public ApiResponse<BlockSliceResDTO> getMyBlocks(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long me = SecurityUtils.getCurrentMemberId();
        BlockSliceResDTO response = blockUsecase.getMyBlocks(me, pageable);
        return ApiResponse.response(HttpStatus.OK, MY_BLOCK_LIST_READ.getMessage(), response);
    }
}
