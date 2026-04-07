package com.tavemakers.surf.domain.home.controller;

import com.tavemakers.surf.domain.home.usecase.HomeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.home.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "홈 배너 관리", description = "홈 배너 관리 API")
public class HomeBannerDeleteController {

    private final HomeUsecase homeUsecase;

    @Operation(summary = "홈 배너 삭제", description = "특정 ID의 홈 배너를 삭제합니다.")
    @DeleteMapping("/v1/admin/home/banners/{bannerId}")
    public ApiResponse<Void> deleteBanner(
            @PathVariable Long bannerId
    ) {
        homeUsecase.deleteBanner(bannerId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, HOME_BANNER_DELETED.getMessage());
    }
}
