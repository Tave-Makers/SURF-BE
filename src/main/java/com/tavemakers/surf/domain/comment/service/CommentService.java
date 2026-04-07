package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.comment.dto.request.CommentCreateReqDTO;
import com.tavemakers.surf.domain.comment.dto.response.CommentListResDTO;
import com.tavemakers.surf.domain.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.domain.comment.dto.response.MentionResDTO;
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
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.service.post.PostGetService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostGetService postGetService;
    private final MemberGetService memberGetService;
    private final CommentMentionService commentMentionService;
    private final CommentLikeService commentLikeService;
    private final CommentLikeRepository commentLikeRepository;

    private final ApplicationEventPublisher eventPublisher;

    /** 댓글 작성 */
    @Transactional
    @LogEvent(value = "comment.create", message = "댓글 생성 성공")
    public CommentResDTO createComment(
            @LogParam("post_id") Long postId,
            Long memberId, CommentCreateReqDTO req) {
        Post post = postGetService.getPost(postId);
        Member member = memberGetService.getMember(memberId);
        if (req.content() == null || req.content().isEmpty()) throw new InvalidBlankCommentException();

        // 댓글 생성 (루트/대댓글 분기)
        Comment saved;

        // 1) 루트 댓글 (parentId == null)
        if (req.parentId() == null) {

            // 루트 댓글 생성
            Comment comment = Comment.root(post, member, req.content());
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
            Comment parent = commentRepository.findById(req.parentId())
                    .orElseThrow(CommentNotFoundException::new);

            // 다른 게시글의 루트 댓글이면 안됨
            if (!parent.getPost().getId().equals(postId))
                throw new CommentNotFoundException();

            // 대댓글은 부모 작성자 자동 멘션 필수
            Long parentWriterId = parent.getMember().getId();

            // 본인 댓글에 대댓글을 다는 경우는 자기 멘션 검증 스킵
            if (!parentWriterId.equals(memberId)) {
                if (req.mentionMemberIds() == null ||
                        !req.mentionMemberIds().contains(parentWriterId)) {
                    throw new InvalidReplyException(); // 대댓글은 부모 멘션 필수
                }
            }

            Comment child = Comment.child(post, member, req.content(), parent);
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
        commentMentionService.createMentions(saved, req.mentionMemberIds());

        // 댓글 수 증가
        post.increaseCommentCount();

        // 응답 DTO (멘션, 좋아요 포함)
        List<MentionResDTO> mentions = commentMentionService.getMentions(saved.getId());
        boolean liked = false; // 새 댓글은 기본적으로 좋아요 없음
        return CommentResDTO.from(saved, postId, mentions, liked);
    }

    /** 댓글 삭제 */
    @Transactional
    @LogEvent("comment.delete")
    public void deleteComment(
            Long postId,
            @LogParam("comment_id") Long commentId,
            Long memberId
    ) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 본인이 쓴 댓글인지 확인
        if (!comment.getPost().getId().equals(postId) || !comment.getMember().getId().equals(memberId))
            throw new NotMyCommentException();

        // 삭제 전에 post 참조를 영속성 컨텍스트에 확보
        Post post = postGetService.getPost(postId);

        // 자식 댓글 parent 끊기
        commentRepository.detachChildren(commentId);

        // 연관 엔티티 먼저 삭제
        commentLikeRepository.deleteAllByComment(comment);
        commentMentionService.deleteAllByComment(comment);

        // 댓글 하드 삭제
        commentRepository.delete(comment);

        // 게시글 댓글 수 감소
        post.decreaseCommentCount();
    }

    /** 댓글 목록 조회 */
    @Transactional(readOnly = true)
    public CommentListResDTO getComments(Long postId, Pageable pageable, Long memberId) {

        // 1) 댓글 Slice 조회
        Slice<Comment> commentSlice =
                commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);

        // 2) 댓글 총 개수 조회
        long totalCount = commentRepository.countByPostId(postId);

        // 3) 각 댓글 → DTO 변환 (멘션 일괄 조회로 N+1 방지)
        List<Comment> comments = commentSlice.getContent();
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        Map<Long, List<MentionResDTO>> mentionMap = commentMentionService.getMentionsByCommentIds(commentIds);

        List<CommentResDTO> commentDtoList = comments.stream()
                .map(comment -> {
                    List<MentionResDTO> mentions = mentionMap.getOrDefault(comment.getId(), List.of());
                    boolean liked = memberId != null && commentLikeService.isLikedByMe(comment.getId(), memberId);
                    return CommentResDTO.from(comment, postId, mentions, liked);
                })
                .toList();

        // 4) CommentListResDTO로 감싸서 반환
        return new CommentListResDTO(
                commentDtoList,
                totalCount,
                commentSlice.hasNext()
        );
    }
}
