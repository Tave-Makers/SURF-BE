package com.tavemakers.surf.domain.post.service.like;

import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.application.query.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostLike;
import com.tavemakers.surf.domain.post.event.PostLikedEvent;
import com.tavemakers.surf.domain.post.exception.PostLikeAlreadyExistsException;
import com.tavemakers.surf.domain.post.exception.PostNotFoundException;
import com.tavemakers.surf.domain.post.repository.PostLikeRepository;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventContext;
import com.tavemakers.surf.global.logging.LogParam;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    private final MemberGetService memberGetService;
    private final ApplicationEventPublisher eventPublisher;

    /** 게시글 좋아요 */
    @Transactional
    @LogEvent(value = "post.like", message = "게시물 좋아요 성공")
    public void like(
            @LogParam("post_id") Long postId,
            @LogParam("user_id") Long memberId) {
        LogEventContext.put("liked", true);

        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        if (post.getBoard().getType() == BoardType.NOTICE) {
            LogEventContext.overrideEvent("notice.like.toggle");
            LogEventContext.overrideMessage("좋아요 버튼 클릭");
        }

        if (postLikeRepository.existsByPostIdAndMemberId(postId, memberId)) {
            return;
        }

        Member member = memberGetService.getMember(memberId);

        try {
            // 즉시 flush해 unique 제약 위반(동시 중복 등록)을 커밋 전에 감지
            postLikeRepository.saveAndFlush(PostLike.of(post, member));
        } catch (DataIntegrityViolationException e) {
            // flush 예외로 트랜잭션이 rollback-only가 되므로 성공 응답은 불가능 — 도메인 예외로 전파해 전체 롤백
            throw new PostLikeAlreadyExistsException();
        }

        createNotificationAtPostLike(member, post.getBoard().getId(), postId);
        // 엔티티 메모리 증감은 동시 요청 시 lost update가 발생하므로 DB 원자적 UPDATE 사용
        postRepository.increaseLikeCount(postId);
    }

    /** 게시글 좋아요 취소 */
    @Transactional
    @LogEvent(value = "post.unlike", message = "게시물 좋아요 해제 성공")
    public void unlike(
            @LogParam("post_id")
            Long postId,
            @LogParam("user_id")
            Long memberId) {
        LogEventContext.put("liked", false);

        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        if (post.getBoard().getType() == BoardType.NOTICE) {
            LogEventContext.overrideEvent("notice.like.toggle");
            LogEventContext.overrideMessage("좋아요 버튼 클릭");
        }

        long deleted = postLikeRepository.deleteByPostIdAndMemberId(postId, memberId);
        if (deleted > 0) {
            postRepository.decreaseLikeCount(postId);
        }
    }

    /** 사용자의 게시글 좋아요 여부 확인 */
    @Transactional(readOnly = true)
    public boolean isLikedByMe(Long memberId, Long postId) {
        return postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
    }

    /** 특정 회원이 누른 게시글 좋아요 전체 제거 — dismiss 전용 bulk 삭제 */
    @Transactional
    public void unlikeAllByMemberId(Long memberId) {
        List<Long> postIds = postLikeRepository.findPostIdsByMemberId(memberId);
        if (postIds.isEmpty()) return;
        postLikeRepository.deleteAllByMemberId(memberId);
        postRepository.decreaseLikeCountBulk(postIds);
    }

    /** 좋아요 생성시 알림 - 게시글 작성자에게 */
    protected void createNotificationAtPostLike(
            Member member,
            Long boardId,
            Long postId
    ) {
        Long postOwnerId = postRepository.findPostOwnerId(postId);

        if (postOwnerId == null || postOwnerId.equals(member.getId())) {
            return;
        }

        eventPublisher.publishEvent(new PostLikedEvent(
                postOwnerId,
                member.getName(),
                member.getId(),
                boardId,
                postId
        ));
    }

}
