package com.tavemakers.surf.domain.scrap.usecase;

import com.tavemakers.surf.domain.post.dto.response.PostResDTO;
import com.tavemakers.surf.domain.scrap.service.ScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스크랩 Usecase */
@Service
@RequiredArgsConstructor
public class ScrapUsecase {

    private final ScrapService scrapService;

    /** 게시글 스크랩 추가 */
    @Transactional
    public void addScrap(Long memberId, Long postId) {
        scrapService.addScrap(memberId, postId);
    }

    /** 게시글 스크랩 삭제 */
    @Transactional
    public void removeScrap(Long memberId, Long postId) {
        scrapService.removeScrap(memberId, postId);
    }

    /** 내 스크랩 목록 조회 */
    @Transactional(readOnly = true)
    public Slice<PostResDTO> getMyScraps(Long memberId, Pageable pageable) {
        return scrapService.getMyScraps(memberId, pageable);
    }
}
