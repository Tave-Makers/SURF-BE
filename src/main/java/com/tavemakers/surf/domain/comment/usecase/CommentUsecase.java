package com.tavemakers.surf.domain.comment.usecase;

import com.tavemakers.surf.domain.comment.dto.request.CommentCreateReqDTO;
import com.tavemakers.surf.domain.comment.dto.response.CommentListResDTO;
import com.tavemakers.surf.domain.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.domain.comment.service.CommentService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** 댓글 Usecase */
@Service
@RequiredArgsConstructor
public class CommentUsecase {

    private final CommentService commentService;
    private final LogEventEmitter logEventEmitter;

    /** 댓글 생성 */
    @Transactional
    public CommentResDTO createComment(Long postId, Long memberId, CommentCreateReqDTO req) {

        CommentResDTO result =
                commentService.createComment(postId, memberId, req);

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

        CommentListResDTO result =
                commentService.getComments(postId, pageable, memberId);

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
