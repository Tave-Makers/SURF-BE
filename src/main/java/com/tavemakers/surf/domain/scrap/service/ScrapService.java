package com.tavemakers.surf.domain.scrap.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.dto.response.PostResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.PostNotFoundException;
import com.tavemakers.surf.domain.post.service.like.PostLikeGetService;
import com.tavemakers.surf.domain.post.service.post.PostGetService;
import com.tavemakers.surf.domain.scrap.entity.Scrap;
import com.tavemakers.surf.domain.scrap.repository.ScrapRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventContext;
import com.tavemakers.surf.global.logging.LogParam;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final PostGetService postGetService;
    private final MemberGetService memberGetService;
    private final PostLikeGetService postLikeGetService;

    /** 게시글 스크랩 추가 */
    @Transactional
    @LogEvent(value = "scrap.add", message = "스크랩 추가 성공")
    public void addScrap(
            @LogParam("user_id") Long memberId,
            @LogParam("post_id") Long postId) {
        Member member = memberGetService.getMember(memberId);
        Post post = postGetService.getPost(postId);
        try {
            scrapRepository.save(Scrap.of(member, post));
            // 버전 기반 단일 UPDATE (+재시도)
            for (int i = 0; i < 3; i++) {
                Long v = postGetService.findVersionById(postId);
                if (v == null) throw new PostNotFoundException();
                if (postGetService.increaseScrapCount(postId, v) > 0) break;
                if (i == 2) throw new OptimisticLockException("scrapCount 증가 충돌");
            }
        } catch (DataIntegrityViolationException e) {
            // 이미 스크랩되어 있으면 무시(멱등)
        }
    }

    /** 게시글 스크랩 삭제 */
    @Transactional
    @LogEvent(value = "scrap.remove", message = "스크랩 삭제 성공")
    public void removeScrap(
            @LogParam("user_id") Long memberId,
            @LogParam("post_id") Long postId) {
        int deleted = scrapRepository.deleteByMemberIdAndPostId(memberId, postId);
        if (deleted > 0) {
            for (int i = 0; i < 3; i++) {
                Long v = postGetService.findVersionById(postId);
                if (v == null) throw new PostNotFoundException();
                if (postGetService.decreaseScrapCount(postId, v) > 0) break;
                if (i == 2) throw new OptimisticLockException("scrapCount 감소 충돌");
            }
        }
    }

    /** 내 스크랩 목록 조회 */
    @LogEvent(value = "scrap.list.view", message = "스크랩 목록 조회")
    public Slice<PostResDTO> getMyScraps(Long memberId, Pageable pageable) {
        Slice<Post> slice = scrapRepository.findPostsByMemberId(memberId, pageable);

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

    /** 특정 회원의 스크랩 전체 제거 */
    @Transactional
    public void removeAllByMemberId(Long memberId) {
        List<Long> postIds = scrapRepository.findPostIdsByMemberId(memberId);
        for (Long postId : postIds) {
            removeScrap(memberId, postId);
        }
    }
}
