package com.tavemakers.surf.application.comment.usecase;

import com.tavemakers.surf.presentation.comment.dto.request.CommentCreateReqDTO;
import com.tavemakers.surf.presentation.comment.dto.response.CommentListResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.MentionResDTO;
import com.tavemakers.surf.application.comment.query.CommentGetService;
import com.tavemakers.surf.application.comment.query.CommentMentionGetService;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.service.CommentService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 댓글 Usecase — 트랜잭션 경계를 소유하고 도메인 서비스 결과(엔티티)를 표현형(DTO)으로 매핑한다.
 * 도메인 계층은 DTO를 알지 못한다.
 */
@Service
@RequiredArgsConstructor
public class CommentUsecase {

    private final CommentService commentService;
    private final CommentGetService commentGetService;
    private final CommentMentionGetService commentMentionGetService;
    private final LogEventEmitter logEventEmitter;

    /** 댓글 생성 */
    @Transactional
    public CommentResDTO createComment(Long postId, Long memberId, CommentCreateReqDTO req) {

        Comment saved = commentService.createComment(
                postId, memberId, req.parentId(), req.content(), req.mentionMemberIds());

        // 응답 DTO (멘션 포함, 새 댓글은 기본적으로 좋아요 없음)
        List<MentionResDTO> mentions = commentMentionGetService.getMentions(saved.getId());
        CommentResDTO result = CommentResDTO.from(saved, postId, mentions, false);

        logEventEmitter.emit("comment.create", Map.of(
                "post_id", postId,
                "comment_id", result.id()
        ));

        return result;
    }

    /** 댓글 삭제 */
    @Transactional
    @LogEvent(value = "comment.delete", message = "댓글 삭제")
    public void deleteComment(
            @LogParam("post_id") Long postId,
            @LogParam("comment_id") Long commentId,
            Long memberId
    ) {
        commentService.deleteComment(postId, commentId, memberId);
    }

    /** 댓글 목록 조회 */
    @Transactional(readOnly = true)
    public CommentListResDTO getComments(Long postId, Pageable pageable, Long memberId) {

        CommentListResDTO result = commentGetService.getComments(postId, pageable, memberId);

        // 첫 진입 제외, 더보기만 로그
        if (pageable.getPageNumber() > 0) {

            logEventEmitter.emit("comment.list.expand", Map.of(
                    "post_id", postId,
                    "loaded_count", result.comments().size()
            ));
        }

        return result;
    }
}
