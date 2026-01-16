package com.tavemakers.surf.domain.comment.facade;

import com.tavemakers.surf.domain.comment.dto.req.CommentCreateReqDTO;
import com.tavemakers.surf.domain.comment.dto.res.CommentLikeMemberResDTO;
import com.tavemakers.surf.domain.comment.dto.res.CommentListResDTO;
import com.tavemakers.surf.domain.comment.dto.res.CommentResDTO;
import com.tavemakers.surf.domain.comment.dto.res.MentionSearchResDTO;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.domain.comment.service.CommentMentionService;
import com.tavemakers.surf.domain.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommentFacade {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final CommentMentionService commentMentionService;

    // CommentService
    public CommentResDTO createComment(Long postId, Long memberId, CommentCreateReqDTO req) {
        return commentService.createComment(postId, memberId, req);
    }

    public CommentListResDTO getComments(Long postId, Pageable pageable, Long memberId) {
        return commentService.getComments(postId, pageable, memberId);
    }

    public void deleteComment(Long postId, Long commentId, Long memberId) {
        commentService.deleteComment(postId, commentId, memberId);
    }

    // CommentLikeService
    public boolean toggleLike(Long commentId, Long memberId) {
        return commentLikeService.toggleLike(commentId, memberId);
    }

    public long countLikes(Long commentId) {
        return commentLikeService.countLikes(commentId);
    }

    public boolean isLikedByMe(Long commentId, Long memberId) {
        return commentLikeService.isLikedByMe(commentId, memberId);
    }

    public List<CommentLikeMemberResDTO> getMembersWhoLiked(Long commentId) {
        return commentLikeService.getMembersWhoLiked(commentId);
    }

    // CommentMentionService
    public List<MentionSearchResDTO> searchMentionableMembers(String keyword) {
        return commentMentionService.searchMentionableMembers(keyword);
    }
}
