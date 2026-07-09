package com.tavemakers.surf.domain.home.service;

import com.tavemakers.surf.domain.home.entity.HomeContent;
import com.tavemakers.surf.domain.home.repository.HomeContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 홈 콘텐츠 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(HomeUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class HomeContentService {

    public static final Long HOME_CONTENT_ID = 1L;

    private final HomeContentRepository homeContentRepository;

    /** 홈 콘텐츠 생성 또는 수정 */
    public HomeContent upsertContent(String message, String sender) {
        return homeContentRepository.findById(HOME_CONTENT_ID)
                .map(existing -> {
                    existing.changeHomeContent(message, sender);
                    return existing;
                })
                .orElseGet(() -> homeContentRepository.save(HomeContent.of(message, sender)));
    }

    /** 홈 콘텐츠 조회 (없으면 empty) */
    public Optional<HomeContent> getContent() {
        return homeContentRepository.findById(HOME_CONTENT_ID);
    }
}
