package com.tavemakers.surf.domain.home.domain.service;

import com.tavemakers.surf.domain.home.presentation.dto.request.HomeContentUpsertReqDTO;
import com.tavemakers.surf.domain.home.presentation.dto.response.HomeContentResDTO;
import com.tavemakers.surf.domain.home.domain.entity.HomeContent;
import com.tavemakers.surf.domain.home.domain.repository.HomeContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeContentService {

    private static final Long HOME_CONTENT_ID = 1L;

    private final HomeContentRepository homeContentRepository;

    /** 홈 콘텐츠 생성 또는 수정 */
    @Transactional
    public HomeContentResDTO upsertContent(HomeContentUpsertReqDTO req) {

        HomeContent content = homeContentRepository.findById(HOME_CONTENT_ID)
                .map(existing -> {
                    existing.changeHomeContent(req.message(), req.sender());
                    return existing;
                })
                .orElseGet(() -> homeContentRepository.save(HomeContent.of(req.message(), req.sender())));

        return HomeContentResDTO.from(content);
    }

    /** 홈 콘텐츠 조회 */
    @Transactional(readOnly = true)
    public HomeContentResDTO getContent() {
        return homeContentRepository.findById(HOME_CONTENT_ID)
                .map(HomeContentResDTO::from)
                .orElse(new HomeContentResDTO(HOME_CONTENT_ID, "", ""));
    }
}