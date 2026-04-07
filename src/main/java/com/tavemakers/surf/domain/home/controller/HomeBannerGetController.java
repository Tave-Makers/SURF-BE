package com.tavemakers.surf.domain.home.controller;

import com.tavemakers.surf.domain.home.dto.response.HomeBannerResDTO;
import com.tavemakers.surf.domain.home.usecase.HomeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.home.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "홈 배너 관리", description = "홈 배너 관리 API")
public class HomeBannerGetController {

    private final HomeUsecase homeUsecase;

    @Operation(summary = "홈 배너 목록 조회", description = "홈 배너 목록을 조회합니다.")
    @GetMapping("/v1/admin/home/banners")
    public ApiResponse<List<HomeBannerResDTO>> getBanners() {
        List<HomeBannerResDTO> response = homeUsecase.getBanners();
        return ApiResponse.response(HttpStatus.OK, HOME_BANNERS_READ.getMessage(), response);
    }
}
