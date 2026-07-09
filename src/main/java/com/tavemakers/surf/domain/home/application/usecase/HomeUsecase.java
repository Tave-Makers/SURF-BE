package com.tavemakers.surf.domain.home.application.usecase;

import com.tavemakers.surf.domain.home.presentation.dto.request.HomeBannerCreateReqDTO;
import com.tavemakers.surf.domain.home.presentation.dto.request.HomeBannerReorderReqDTO;
import com.tavemakers.surf.domain.home.presentation.dto.request.HomeBannerUpdateReqDTO;
import com.tavemakers.surf.domain.home.presentation.dto.request.HomeContentUpsertReqDTO;
import com.tavemakers.surf.domain.home.presentation.dto.response.HomeBannerResDTO;
import com.tavemakers.surf.domain.home.presentation.dto.response.HomeContentResDTO;
import com.tavemakers.surf.domain.home.presentation.dto.response.HomeResDTO;
import com.tavemakers.surf.domain.home.application.query.HomeGetService;
import com.tavemakers.surf.domain.home.domain.service.HomeBannerService;
import com.tavemakers.surf.domain.home.domain.service.HomeContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 홈 Usecase — 트랜잭션 경계를 소유하고 도메인 서비스 결과(엔티티)를 표현형(DTO)으로 매핑한다.
 * 도메인 계층은 DTO를 알지 못한다.
 */
@Service
@RequiredArgsConstructor
public class HomeUsecase {

    private final HomeGetService homeGetService;
    private final HomeBannerService homeBannerService;
    private final HomeContentService homeContentService;

    /** 홈 화면 조회 */
    @Transactional(readOnly = true)
    public HomeResDTO getHome() {
        return homeGetService.getHome();
    }

    /** 배너 생성 */
    @Transactional
    public HomeBannerResDTO createBanner(HomeBannerCreateReqDTO req) {
        return HomeBannerResDTO.from(
                homeBannerService.createBanner(req.name(), req.imageUrl(), req.linkUrl()));
    }

    /** 배너 목록 조회 */
    @Transactional(readOnly = true)
    public List<HomeBannerResDTO> getBanners() {
        return homeBannerService.getBanners().stream()
                .map(HomeBannerResDTO::from)
                .toList();
    }

    /** 배너 삭제 */
    @Transactional
    public void deleteBanner(Long bannerId) {
        homeBannerService.deleteBanner(bannerId);
    }

    /** 배너 순서 변경 */
    @Transactional
    public List<HomeBannerResDTO> reorderBanners(HomeBannerReorderReqDTO req) {
        return homeBannerService.reorderBanners(req.orderedIds()).stream()
                .map(HomeBannerResDTO::from)
                .toList();
    }

    /** 배너 수정 */
    @Transactional
    public HomeBannerResDTO updateBanner(Long bannerId, HomeBannerUpdateReqDTO req) {
        return HomeBannerResDTO.from(
                homeBannerService.updateBanner(bannerId, req.name(), req.imageUrl(), req.linkUrl()));
    }

    /** 배너 활성화 */
    @Transactional
    public HomeBannerResDTO activateBanner(Long bannerId) {
        return HomeBannerResDTO.from(homeBannerService.activateBanner(bannerId));
    }

    /** 배너 비활성화 */
    @Transactional
    public HomeBannerResDTO deactivateBanner(Long bannerId) {
        return HomeBannerResDTO.from(homeBannerService.deactivateBanner(bannerId));
    }

    /** 홈 콘텐츠 Upsert */
    @Transactional
    public HomeContentResDTO upsertContent(HomeContentUpsertReqDTO req) {
        return HomeContentResDTO.from(
                homeContentService.upsertContent(req.message(), req.sender()));
    }

    /** 홈 콘텐츠 조회 (없으면 빈 기본값) */
    @Transactional(readOnly = true)
    public HomeContentResDTO getContent() {
        return homeContentService.getContent()
                .map(HomeContentResDTO::from)
                .orElse(new HomeContentResDTO(HomeContentService.HOME_CONTENT_ID, "", ""));
    }
}
