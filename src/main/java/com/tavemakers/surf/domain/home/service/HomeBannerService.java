package com.tavemakers.surf.domain.home.service;

import com.tavemakers.surf.domain.home.dto.request.HomeBannerCreateReqDTO;
import com.tavemakers.surf.domain.home.dto.request.HomeBannerReorderReqDTO;
import com.tavemakers.surf.domain.home.dto.request.HomeBannerUpdateReqDTO;
import com.tavemakers.surf.domain.home.dto.response.HomeBannerResDTO;
import com.tavemakers.surf.domain.home.entity.HomeBanner;
import com.tavemakers.surf.domain.home.exception.AllHomeBannersRequiredException;
import com.tavemakers.surf.domain.home.exception.EmptyHomeBannersException;
import com.tavemakers.surf.domain.home.exception.HomeBannerNotFoundException;
import com.tavemakers.surf.domain.home.exception.InvalidHomeBannerRequestException;
import com.tavemakers.surf.domain.home.repository.HomeBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeBannerService {

    private final HomeBannerRepository homeBannerRepository;

    /** 홈 배너 생성 */
    @Transactional
    public HomeBannerResDTO createBanner(HomeBannerCreateReqDTO req) {
        int nextOrder = homeBannerRepository.findMaxDisplayOrder().orElse(0) + 1;

        HomeBanner banner = HomeBanner.of(req.name(), req.imageUrl(), req.linkUrl(), nextOrder);

        return HomeBannerResDTO.from(homeBannerRepository.save(banner));
    }

    /** 홈 배너 목록 조회 */
    @Transactional(readOnly = true)
    public List<HomeBannerResDTO> getBanners() {
        return homeBannerRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(HomeBannerResDTO::from)
                .toList();
    }

    /** 홈 배너 삭제 */
    @Transactional
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

    /** 홈 배너 순서 변경 */
    @Transactional
    public List<HomeBannerResDTO> reorderBanners(HomeBannerReorderReqDTO req) {
        List<Long> orderedIds = req.orderedIds();

        List<HomeBanner> banners = validateAndLoadAllBanners(orderedIds);
        if (banners.isEmpty()) return List.of();

        Map<Long, HomeBanner> map = banners.stream()
                .collect(Collectors.toMap(HomeBanner::getId, b -> b));

        int displayOrder = 1;
        for (Long id : orderedIds) {
            map.get(id).changeDisplayOrder(displayOrder++);
        }

        return homeBannerRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(HomeBannerResDTO::from)
                .toList();
    }

    /** 홈 배너 수정 */
    @Transactional
    public HomeBannerResDTO updateBanner(Long bannerId, HomeBannerUpdateReqDTO req) {
        HomeBanner banner = homeBannerRepository.findById(bannerId)
                .orElseThrow(HomeBannerNotFoundException::new);

        banner.updateBanner(req.name(), req.imageUrl(), req.linkUrl());

        return HomeBannerResDTO.from(banner);
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

    @Transactional
    public HomeBannerResDTO activateBanner(Long bannerId) {
        HomeBanner banner = findBanner(bannerId);
        banner.activate();
        return HomeBannerResDTO.from(banner);
    }

    @Transactional
    public HomeBannerResDTO deactivateBanner(Long bannerId) {
        HomeBanner banner = findBanner(bannerId);
        banner.deactivate();
        return HomeBannerResDTO.from(banner);
    }

    private HomeBanner findBanner(Long id) {
        return homeBannerRepository.findById(id)
                .orElseThrow(HomeBannerNotFoundException::new);
    }
}
