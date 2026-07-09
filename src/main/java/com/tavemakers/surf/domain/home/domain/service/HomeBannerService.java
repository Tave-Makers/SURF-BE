package com.tavemakers.surf.domain.home.domain.service;

import com.tavemakers.surf.domain.home.domain.entity.HomeBanner;
import com.tavemakers.surf.domain.home.domain.exception.AllHomeBannersRequiredException;
import com.tavemakers.surf.domain.home.domain.exception.EmptyHomeBannersException;
import com.tavemakers.surf.domain.home.domain.exception.HomeBannerNotFoundException;
import com.tavemakers.surf.domain.home.domain.exception.InvalidHomeBannerRequestException;
import com.tavemakers.surf.domain.home.domain.repository.HomeBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 홈 배너 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(HomeUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class HomeBannerService {

    private final HomeBannerRepository homeBannerRepository;

    /** 홈 배너 생성 (맨 뒤 순서로) */
    public HomeBanner createBanner(String name, String imageUrl, String linkUrl) {
        int nextOrder = homeBannerRepository.findMaxDisplayOrder().orElse(0) + 1;
        return homeBannerRepository.save(HomeBanner.of(name, imageUrl, linkUrl, nextOrder));
    }

    /** 홈 배너 목록 조회 (순서 오름차순) */
    public List<HomeBanner> getBanners() {
        return homeBannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    /** 홈 배너 삭제 후 남은 배너를 1부터 재정렬 */
    public void deleteBanner(Long bannerId) {
        HomeBanner target = homeBannerRepository.findById(bannerId)
                .orElseThrow(HomeBannerNotFoundException::new);

        homeBannerRepository.delete(target);

        List<HomeBanner> remain = homeBannerRepository.findAllByOrderByDisplayOrderAsc();

        // 1부터 다시 정렬
        int order = 1;
        for (HomeBanner banner : remain) {
            if (!banner.getDisplayOrder().equals(order)) {
                banner.changeDisplayOrder(order);
            }
            order++;
        }
    }

    /** 홈 배너 순서 변경 — orderedIds 순서대로 displayOrder 재부여 */
    public List<HomeBanner> reorderBanners(List<Long> orderedIds) {
        List<HomeBanner> banners = validateAndLoadAllBanners(orderedIds);
        if (banners.isEmpty()) return List.of();

        Map<Long, HomeBanner> map = banners.stream()
                .collect(Collectors.toMap(HomeBanner::getId, b -> b));

        int displayOrder = 1;
        for (Long id : orderedIds) {
            map.get(id).changeDisplayOrder(displayOrder++);
        }

        return homeBannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    /** 홈 배너 수정 */
    public HomeBanner updateBanner(Long bannerId, String name, String imageUrl, String linkUrl) {
        HomeBanner banner = findBanner(bannerId);
        banner.updateBanner(name, imageUrl, linkUrl);
        return banner;
    }

    /** 홈 배너 활성화 */
    public HomeBanner activateBanner(Long bannerId) {
        HomeBanner banner = findBanner(bannerId);
        banner.activate();
        return banner;
    }

    /** 홈 배너 비활성화 */
    public HomeBanner deactivateBanner(Long bannerId) {
        HomeBanner banner = findBanner(bannerId);
        banner.deactivate();
        return banner;
    }

    private List<HomeBanner> validateAndLoadAllBanners(List<Long> orderedIds) {
        long total = homeBannerRepository.count();

        // 배너가 없는 경우: 요청도 비어 있어야 정상
        if (total == 0) {
            if (!orderedIds.isEmpty()) {
                throw new EmptyHomeBannersException();
            }
            return List.of();
        }

        // 배너가 있는데 요청이 비어있으면 에러
        if (orderedIds.isEmpty()) {
            throw new AllHomeBannersRequiredException();
        }

        // 전체 포함 검증
        if (orderedIds.size() != total) {
            throw new AllHomeBannersRequiredException();
        }

        // 중복 id 검증
        if (orderedIds.stream().distinct().count() != orderedIds.size()) {
            throw new InvalidHomeBannerRequestException();
        }

        List<HomeBanner> banners = homeBannerRepository.findAllById(orderedIds);

        // 존재하지 않는 id 검증
        if (banners.size() != orderedIds.size()) {
            throw new InvalidHomeBannerRequestException();
        }

        return banners;
    }

    private HomeBanner findBanner(Long id) {
        return homeBannerRepository.findById(id)
                .orElseThrow(HomeBannerNotFoundException::new);
    }
}
