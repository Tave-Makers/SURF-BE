package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.repository.CommentLikeRepository;
import com.tavemakers.surf.domain.comment.repository.CommentMentionRepository;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 댓글 삭제 전용 서비스 */
@Service
@RequiredArgsConstructor
public class CommentDeleteService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentMentionRepository commentMentionRepository;

    /** 게시글의 모든 댓글 삭제 (연관 데이터 먼저 삭제) */
    @Transactional
    public void deleteAllByPostId(Long postId) {
        commentLikeRepository.deleteAllByPostId(postId);
        commentMentionRepository.deleteAllByPostId(postId);
        commentRepository.deleteRepliesByPostId(postId);
        commentRepository.deleteRootCommentsByPostId(postId);
    }

    /** 특정 회원이 남긴 댓글 삭제 (이미 삭제 예정인 게시글은 제외) */
    @Transactional
    public void deleteAllByMemberId(Long memberId, Set<Long> deletedPostIds) {
        for (Comment comment : commentRepository.findAllByMemberId(memberId)) {
            if (deletedPostIds.contains(comment.getPost().getId())) {
                continue;
            }
            deleteComment(comment);
        }
    }

    /** 댓글 단건 강제 삭제 */
    @Transactional
    public void deleteComment(Comment comment) {
        commentRepository.detachChildren(comment.getId());
        commentLikeRepository.deleteAllByComment(comment);
        commentMentionRepository.deleteAllByComment(comment);
        commentRepository.delete(comment);
        comment.getPost().decreaseCommentCount();
    }
}
