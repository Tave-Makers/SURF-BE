package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.event.CommentCreatedEvent;
import com.tavemakers.surf.domain.comment.event.CommentReplyEvent;
import com.tavemakers.surf.domain.comment.exception.CommentNotFoundException;
import com.tavemakers.surf.domain.comment.exception.InvalidBlankCommentException;
import com.tavemakers.surf.domain.comment.exception.InvalidReplyException;
import com.tavemakers.surf.domain.comment.exception.NotMyCommentException;
import com.tavemakers.surf.domain.comment.repository.CommentLikeRepository;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.post.service.support.PostCommentCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 댓글 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(CommentUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostGetService postGetService;
    private final MemberGetService memberGetService;
    private final CommentMentionService commentMentionService;
    private final CommentLikeRepository commentLikeRepository;
    private final PostCommentCountService postCommentCountService;

    private final ApplicationEventPublisher eventPublisher;

    /** 댓글 작성 (루트/대댓글 분기). 저장된 댓글 엔티티를 반환한다. */
    public Comment createComment(
            Long postId,
            Long memberId,
            Long parentId,
            String content,
            List<Long> mentionMemberIds) {
        Post post = postGetService.getPost(postId);
        Member member = memberGetService.getMember(memberId);
        if (content == null || content.isEmpty()) throw new InvalidBlankCommentException();

        // 댓글 생성 (루트/대댓글 분기)
        Comment saved;

        // 1) 루트 댓글 (parentId == null)
        if (parentId == null) {

            // 루트 댓글 생성
            Comment comment = Comment.root(post, member, content);
            saved = commentRepository.save(comment);
            saved.markAsRoot();

            if(!post.getMember().getId().equals(memberId)) {
                // 댓글 생성 알림 - 게시글 작성자에게
                eventPublisher.publishEvent(new CommentCreatedEvent(
                        post.getMember().getId(),
                        member.getName(),
                        member.getId(),
                        post.getBoard().getId(),
                        postId
                ));
            }

        } else {

            // 2) 대댓글 생성 (parentId != null)
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(CommentNotFoundException::new);

            // 다른 게시글의 루트 댓글이면 안됨
            if (!parent.getPost().getId().equals(postId))
                throw new CommentNotFoundException();

            // 대댓글은 부모 작성자 자동 멘션 필수
            Long parentWriterId = parent.getMember().getId();

            // 본인 댓글에 대댓글을 다는 경우는 자기 멘션 검증 스킵
            if (!parentWriterId.equals(memberId)) {
                if (mentionMemberIds == null ||
                        !mentionMemberIds.contains(parentWriterId)) {
                    throw new InvalidReplyException(); // 대댓글은 부모 멘션 필수
                }
            }

            Comment child = Comment.child(post, member, content, parent);
            saved = commentRepository.save(child);

            if (!parentWriterId.equals(memberId)) {
                // 대댓글 생성 알림 - 부모 댓글 작성자에게
                eventPublisher.publishEvent(new CommentReplyEvent(
                        parentWriterId,
                        member.getName(),
                        member.getId(),
                        post.getBoard().getId(),
                        postId
                ));
            }
        }
        // 멘션 등록
        commentMentionService.createMentions(saved, mentionMemberIds);

        // 댓글 수 증가 — 동시 요청 lost update 방지 위해 원자적 UPDATE
        postCommentCountService.increase(postId);

        return saved;
    }

    /** 댓글 삭제 */
    public void deleteComment(
            Long postId,
            Long commentId,
            Long memberId
    ) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 본인이 쓴 댓글인지 확인
        if (!comment.getPost().getId().equals(postId) || !comment.getMember().getId().equals(memberId))
            throw new NotMyCommentException();

        // 자식 댓글 parent 끊기
        commentRepository.detachChildren(commentId);

        // 연관 엔티티 먼저 삭제
        commentLikeRepository.deleteAllByComment(comment);
        commentMentionService.deleteAllByComment(comment);

        // 댓글 하드 삭제
        commentRepository.delete(comment);

        // 게시글 댓글 수 감소 — 동시 요청 lost update 방지 위해 원자적 UPDATE
        postCommentCountService.decrease(postId);
    }
}
