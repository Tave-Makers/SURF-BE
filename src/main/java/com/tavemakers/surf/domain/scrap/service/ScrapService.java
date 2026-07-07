package com.tavemakers.surf.domain.scrap.service;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.application.query.MemberGetService;
import com.tavemakers.surf.domain.post.dto.response.PostResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.service.like.PostLikeGetService;
import com.tavemakers.surf.domain.post.service.post.PostGetService;
import com.tavemakers.surf.domain.scrap.entity.Scrap;
import com.tavemakers.surf.domain.scrap.exception.ScrapAlreadyExistsException;
import com.tavemakers.surf.domain.scrap.repository.ScrapRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventContext;
import com.tavemakers.surf.global.logging.LogParam;
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

        // 재클릭(순차 중복)은 여기서 멱등 종료 — 아래 insert의 unique 위반 경로를 애초에 타지 않게 한다
        if (scrapRepository.existsByMemberIdAndPostId(memberId, postId)) {
            return;
        }

        try {
            // 즉시 flush해 동시 중복 요청의 unique 위반을 커밋 전에 감지 (커밋 시점 예외는
            // 트랜잭션을 rollback-only로 만들어 UnexpectedRollbackException을 유발)
            scrapRepository.saveAndFlush(Scrap.of(member, post));
        } catch (DataIntegrityViolationException e) {
            // 동시 중복 스크랩 race의 패자 — flush 예외로 트랜잭션이 rollback-only가 되어
            // 성공 응답이 불가능하므로 도메인 예외(409)로 전파 (순차 재클릭은 위 exists에서 걸러짐)
            throw new ScrapAlreadyExistsException();
        }

        // 엔티티 메모리 증감은 동시 요청 시 lost update가 발생하므로 DB 원자적 UPDATE 사용
        postGetService.increaseScrapCount(postId);
    }

    /** 게시글 스크랩 삭제 */
    @Transactional
    @LogEvent(value = "scrap.remove", message = "스크랩 삭제 성공")
    public void removeScrap(
            @LogParam("user_id") Long memberId,
            @LogParam("post_id") Long postId) {
        int deleted = scrapRepository.deleteByMemberIdAndPostId(memberId, postId);
        if (deleted > 0) {
            postGetService.decreaseScrapCount(postId);
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
