package com.tavemakers.surf.domain.home.usecase;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 홈 Usecase */
@Service
@RequiredArgsConstructor
public class HomeUsecase {

    private final HomeService homeService;
    private final HomeBannerService homeBannerService;
    private final HomeContentService homeContentService;

    /** 홈 화면 조회 */
    @Transactional(readOnly = true)
    public HomeResDTO getHome() {
        return homeService.getHome();
    }

    /** 배너 생성 */
    @Transactional
    public HomeBannerResDTO createBanner(HomeBannerCreateReqDTO req) {
        return homeBannerService.createBanner(req);
    }

    /** 배너 목록 조회 */
    @Transactional(readOnly = true)
    public List<HomeBannerResDTO> getBanners() {
        return homeBannerService.getBanners();
    }

    /** 배너 삭제 */
    @Transactional
    public void deleteBanner(Long bannerId) {
        homeBannerService.deleteBanner(bannerId);
    }

    /** 배너 순서 변경 */
    @Transactional
    public List<HomeBannerResDTO> reorderBanners(HomeBannerReorderReqDTO req) {
        return homeBannerService.reorderBanners(req);
    }

    /** 배너 수정 */
    @Transactional
    public HomeBannerResDTO updateBanner(Long bannerId, HomeBannerUpdateReqDTO req) {
        return homeBannerService.updateBanner(bannerId, req);
    }

    /** 배너 활성화 */
    @Transactional
    public HomeBannerResDTO activateBanner(Long bannerId) {
        return homeBannerService.activateBanner(bannerId);
    }

    /** 배너 비활성화 */
    @Transactional
    public HomeBannerResDTO deactivateBanner(Long bannerId) {
        return homeBannerService.deactivateBanner(bannerId);
    }

    /** 홈 콘텐츠 Upsert */
    @Transactional
    public HomeContentResDTO upsertContent(HomeContentUpsertReqDTO req) {
        return homeContentService.upsertContent(req);
    }

    /** 홈 콘텐츠 조회 */
    @Transactional(readOnly = true)
    public HomeContentResDTO getContent() {
        return homeContentService.getContent();
    }
}
