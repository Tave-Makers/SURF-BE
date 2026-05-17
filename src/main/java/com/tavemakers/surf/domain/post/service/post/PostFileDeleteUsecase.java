package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.event.PostFilesDeletedEvent;
import com.tavemakers.surf.domain.post.exception.PostFileDeleteAccessDeniedException;
import com.tavemakers.surf.domain.post.service.file.PostFileDeleteService;
import com.tavemakers.surf.domain.post.service.file.PostFileGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 게시글 첨부파일 삭제 Usecase - DB 삭제 후 커밋 시점에 S3 파일 삭제 */
@Service
@RequiredArgsConstructor
public class PostFileDeleteUsecase {

    private final PostFileGetService postFileGetService;
    private final PostFileDeleteService postFileDeleteService;
    private final MemberGetService memberGetService;
    private final ApplicationEventPublisher eventPublisher;

    /** 게시글 첨부파일 단건 삭제 (작성자 또는 관리자만 허용) */
    @Transactional
    public void deletePostFile(Long fileId, Long requesterId) {
        PostFileUrl file = postFileGetService.getPostFileUrl(fileId);
        Member member = memberGetService.getMember(requesterId);
        validateOwnerOrManager(file.getPost(), member);

        postFileDeleteService.delete(file);
        eventPublisher.publishEvent(new PostFilesDeletedEvent(List.of(file.getFileUrl())));
    }

    /** 게시글 소유자 또는 관리자 권한 검증 */
    private void validateOwnerOrManager(Post post, Member member) {
        if (!member.hasDeleteRole() && !post.isOwner(member.getId())) {
            throw new PostFileDeleteAccessDeniedException();
        }
    }
}
