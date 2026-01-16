package com.tavemakers.surf.domain.home.facade;

import com.tavemakers.surf.domain.home.dto.request.HomeBannerCreateReqDTO;
import com.tavemakers.surf.domain.home.dto.request.HomeBannerReorderReqDTO;
import com.tavemakers.surf.domain.home.dto.request.HomeBannerUpdateReqDTO;
import com.tavemakers.surf.domain.home.dto.request.HomeContentUpsertReqDTO;
import com.tavemakers.surf.domain.home.dto.response.HomeBannerResDTO;
import com.tavemakers.surf.domain.home.dto.response.HomeContentResDTO;
import com.tavemakers.surf.domain.home.dto.response.HomeResDTO;
import com.tavemakers.surf.domain.home.service.HomeBannerService;
import com.tavemakers.surf.domain.home.service.HomeContentService;
import com.tavemakers.surf.domain.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HomeFacade {

    private final HomeService homeService;
    private final HomeBannerService homeBannerService;
    private final HomeContentService homeContentService;

    // HomeService
    public HomeResDTO getHome() {
        return homeService.getHome();
    }

    // HomeBannerService
    public List<HomeBannerResDTO> getBanners() {
        return homeBannerService.getBanners();
    }

    public HomeBannerResDTO createBanner(HomeBannerCreateReqDTO req) {
        return homeBannerService.createBanner(req);
    }

    public void deleteBanner(Long bannerId) {
        homeBannerService.deleteBanner(bannerId);
    }

    public List<HomeBannerResDTO> reorderBanners(HomeBannerReorderReqDTO req) {
        return homeBannerService.reorderBanners(req);
    }

    public HomeBannerResDTO updateBanner(Long bannerId, HomeBannerUpdateReqDTO req) {
        return homeBannerService.updateBanner(bannerId, req);
    }

    // HomeContentService
    public HomeContentResDTO getContent() {
        return homeContentService.getContent();
    }

    public HomeContentResDTO upsertContent(HomeContentUpsertReqDTO req) {
        return homeContentService.upsertContent(req);
    }
}
