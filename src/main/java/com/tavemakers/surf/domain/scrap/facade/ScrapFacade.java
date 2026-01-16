package com.tavemakers.surf.domain.scrap.facade;

import com.tavemakers.surf.domain.post.dto.res.PostResDTO;
import com.tavemakers.surf.domain.scrap.service.ScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScrapFacade {
    private final ScrapService scrapService;

    public void addScrap(Long memberId, Long postId) {
        scrapService.addScrap(memberId, postId);
    }

    public void removeScrap(Long memberId, Long postId) {
        scrapService.removeScrap(memberId, postId);
    }

    public Slice<PostResDTO> getMyScraps(Long memberId, Pageable pageable) {
        return scrapService.getMyScraps(memberId, pageable);
    }

    public boolean isScrappedByMe(Long memberId, Long postId) {
        return scrapService.isScrappedByMe(memberId, postId);
    }
}
