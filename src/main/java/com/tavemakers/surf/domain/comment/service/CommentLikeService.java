package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.comment.dto.response.CommentLikeMemberResDTO;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.entity.CommentLike;
import com.tavemakers.surf.domain.comment.event.CommentLikedEvent;
import com.tavemakers.surf.domain.comment.exception.CommentLikeAlreadyExistsException;
import com.tavemakers.surf.domain.comment.exception.CommentNotFoundException;
import com.tavemakers.surf.domain.comment.repository.CommentLikeRepository;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.service.post.PostGetService;

import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final MemberGetService memberGetService;
    private final PostGetService postGetService;
    private final LogEventEmitter logEventEmitter;

    private final ApplicationEventPublisher eventPublisher;

    /** 좋아요 및 좋아요 취소 */
    @Transactional
    public boolean toggleLike(@LogParam("comment_id") Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
        Member member = memberGetService.getMember(memberId);

        Post post = postGetService.getPost(comment.getPost().getId());

        // 좋아요 이미 존재하면 취소
        int removed = commentLikeRepository.deleteByCommentAndMember(comment, member);
        if (removed > 0) {
            // 엔티티 메모리 증감은 동시 요청 시 lost update가 발생하므로 DB 원자적 UPDATE 사용
            commentRepository.decreaseLikeCount(commentId);

            logEventEmitter.emit("comment.like.toggle", Map.of(
                    "comment_id", commentId,
                    "liked", false
            ));

            return false; // 이미 눌러져 있었던 거라서 좋아요 취소됨
        }

        try {
            // 즉시 flush해 unique 제약 위반(동시 중복 등록)을 커밋 전에 감지
            commentLikeRepository.saveAndFlush(CommentLike.of(comment, member));
        } catch (DataIntegrityViolationException e) {
            // flush 예외로 트랜잭션이 rollback-only가 되므로 성공 응답은 불가능 — 도메인 예외로 전파해 전체 롤백
            throw new CommentLikeAlreadyExistsException();
        }

        commentRepository.increaseLikeCount(commentId);
        createNotificationAtCommentLike(member, commentId, post.getBoard().getId(), post.getId());

        logEventEmitter.emit("comment.like.toggle", Map.of(
                "comment_id", commentId,
                "liked", true
        ));

        return true; // 새로 좋아요 등록
    }

    /** 댓글의 총 좋아요 수 */
    @Transactional(readOnly = true)
    public long countLikes(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
        return commentLikeRepository.countByComment(comment);
    }

    /** 내가 해당 댓글에 좋아요 눌렀는지 여부 */
    @Transactional(readOnly = true)
    public boolean isLikedByMe(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
        Member member = memberGetService.getMember(memberId);
        return commentLikeRepository.existsByCommentAndMember(comment, member);
    }

    /** 특정 댓글에 좋아요를 누른 회원들의 ID, 이름, 프로필 이미지를 조회 */
    @Transactional(readOnly = true)
    public List<CommentLikeMemberResDTO> getMembersWhoLiked(Long commentId) {
        List<Member> members = commentLikeRepository.findMembersWhoLiked(commentId);
        return members.stream()
                .map(member -> new CommentLikeMemberResDTO(
                        member.getId(),
                        member.getName(),
                        member.getProfileImageUrl()))
                .toList();
    }

    /** 특정 회원이 누른 댓글 좋아요 전체 제거 */
    @Transactional
    public void removeAllByMemberId(Long memberId) {
        for (CommentLike commentLike : commentLikeRepository.findAllByMemberId(memberId)) {
            Comment comment = commentLike.getComment();
            commentLikeRepository.delete(commentLike);
            commentRepository.decreaseLikeCount(comment.getId());
        }
    }

    /** 좋아요 생성시 알림 - 댓글 작성자에게 */
    protected void createNotificationAtCommentLike(
            Member member,
            Long commentId,
            Long boardId,
            Long postId
    ) {
        Long commentOwnerId = commentRepository.findCommentOwnerId(commentId);

        if (commentOwnerId == null) {
            return;
        }

        // 자기 글이면 알림 안 보냄
        if (commentOwnerId.equals(member.getId())) {
            return;
        }

        eventPublisher.publishEvent(new CommentLikedEvent(
                commentOwnerId,
                member.getName(),
                member.getId(),
                boardId,
                postId
        ));
    }

}
