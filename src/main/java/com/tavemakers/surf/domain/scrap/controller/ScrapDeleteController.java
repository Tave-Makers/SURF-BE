package com.tavemakers.surf.domain.scrap.controller;

import com.tavemakers.surf.domain.scrap.usecase.ScrapUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.scrap.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "스크랩", description = "추후 MVP를 통해 디벨롭 될 예정")
public class ScrapDeleteController {

    private final ScrapUsecase scrapUsecase;

    /** 스크랩 삭제 (현재 로그인 사용자 기준) */
    @Operation(summary = "스크랩 삭제", description = "특정 게시글의 스크랩을 취소합니다.")
    @DeleteMapping("/v1/user/scraps/{postId}")
    public ApiResponse<Void> removeScrap(@PathVariable Long postId) {
        Long me = SecurityUtils.getCurrentMemberId();
        scrapUsecase.removeScrap(me, postId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, SCRAP_DELETED.getMessage());
    }
}
