package com.tavemakers.surf.domain.scrap.application.query;

import com.tavemakers.surf.domain.scrap.domain.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapGetService {

    private final ScrapRepository scrapRepository;

    /** 게시글의 모든 스크랩 삭제 */
    @Transactional
    public void deleteByPostId(Long postId) {
        scrapRepository.deleteByPostId(postId);
    }

    /** 스크랩된 게시글 ID 목록 반환 */
    public Set<Long> findScrappedPostIdsByMemberAndPostIds(Long viewerId, Collection<Long> postIds) {
        return scrapRepository.findScrappedPostIdsByMemberAndPostIds(viewerId, postIds);
    }

    /** 해당 게시글 스크랩 여부 확인 */
    public boolean isScrappedByMe(Long memberId, Long postId) {
        return scrapRepository.existsByMemberIdAndPostId(memberId, postId);
    }
}
