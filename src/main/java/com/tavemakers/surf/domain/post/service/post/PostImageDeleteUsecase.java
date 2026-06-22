package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.event.PostFilesDeletedEvent;
import com.tavemakers.surf.domain.post.exception.PostImageDeleteAccessDeniedException;
import com.tavemakers.surf.domain.post.exception.PostImageMismatchException;
import com.tavemakers.surf.domain.post.service.image.PostImageDeleteService;
import com.tavemakers.surf.domain.post.service.image.PostImageGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 게시글 이미지 삭제 Usecase - DB 삭제 후 커밋 시점에 S3 이미지 삭제 */
@Service
@RequiredArgsConstructor
public class PostImageDeleteUsecase {

    private final PostImageGetService postImageGetService;
    private final PostImageDeleteService postImageDeleteService;
    private final MemberGetService memberGetService;
    private final ApplicationEventPublisher eventPublisher;

    /** 게시글 이미지 단건 삭제 (작성자 또는 관리자만 허용) */
    @Transactional
    public void deletePostImage(Long postId, Long imageId, Long requesterId) {
        PostImageUrl image = postImageGetService.getPostImageUrl(imageId);
        validateImageBelongsToPost(image, postId);

        Member member = memberGetService.getMember(requesterId);
        validateOwnerOrManager(image.getPost(), member);

        postImageDeleteService.delete(image);
        eventPublisher.publishEvent(new PostFilesDeletedEvent(List.of(image.getOriginalUrl())));
    }

    /** URL의 postId와 이미지가 실제 속한 게시글이 일치하는지 검증 */
    private void validateImageBelongsToPost(PostImageUrl image, Long postId) {
        if (!image.getPost().getId().equals(postId)) {
            throw new PostImageMismatchException();
        }
    }

    /** 게시글 소유자 또는 관리자 권한 검증 */
    private void validateOwnerOrManager(Post post, Member member) {
        if (!member.hasDeleteRole() && !post.isOwner(member.getId())) {
            throw new PostImageDeleteAccessDeniedException();
        }
    }
}
