package com.tavemakers.surf.domain.scrap.controller;

import com.tavemakers.surf.domain.post.dto.response.PostResDTO;
import com.tavemakers.surf.domain.scrap.usecase.ScrapUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.scrap.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "스크랩", description = "추후 MVP를 통해 디벨롭 될 예정")
public class ScrapGetController {

    private final ScrapUsecase scrapUsecase;

    /** 내가 스크랩한 게시글 목록 */
    @Operation(summary = "내가 스크랩한 게시글 목록", description = "본인이 스크랩한 게시글 목록을 조회합니다.")
    @GetMapping("/v1/user/scraps/me")
    public ApiResponse<Slice<PostResDTO>> myScraps(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Long me = SecurityUtils.getCurrentMemberId();
        Slice<PostResDTO> response = scrapUsecase.getMyScraps(me, pageable);
        return ApiResponse.response(HttpStatus.OK, MY_SCRAP_LIST_READ.getMessage(), response);
    }
}
