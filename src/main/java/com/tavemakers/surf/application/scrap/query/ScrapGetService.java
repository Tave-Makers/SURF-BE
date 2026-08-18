package com.tavemakers.surf.application.scrap.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.post.query.PostLikeGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.scrap.repository.ScrapRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventContext;
import com.tavemakers.surf.presentation.post.dto.response.PostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapGetService {

    private final ScrapRepository scrapRepository;
    private final PostLikeGetService postLikeGetService;
    private final BlockGetService blockGetService;

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

    /** 차단 작성자의 게시글을 제외한 내 스크랩 목록을 조회한다 */
    @LogEvent(value = "scrap.list.view", message = "스크랩 목록 조회")
    public Slice<PostResDTO> getMyScraps(Long memberId, Pageable pageable) {
        Set<Long> excludedAuthorIds = blockGetService.getMyBlockedMemberIds(memberId);

        Slice<Post> slice =
                scrapRepository.findPostsByMemberIdExcludingAuthors(memberId, excludedAuthorIds, pageable);

        List<Long> postIds = slice.getContent().stream()
                .map(Post::getId)
                .toList();

        Set<Long> likedIds = new HashSet<>(postLikeGetService.findLikedPostIdsByMemberAndPostIds(memberId, postIds));

        Slice<PostResDTO> result = slice.map(post ->
                PostResDTO.from(post, true, likedIds.contains(post.getId()))
        );

        LogEventContext.put("count", slice.getNumberOfElements());

        return result;
    }
}
