package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.exception.PostDeleteAccessDeniedException;
import com.tavemakers.surf.domain.post.repository.PostLikeRepository;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.file.PostFileDeleteService;
import com.tavemakers.surf.domain.post.service.file.PostFileGetService;
import com.tavemakers.surf.domain.post.service.image.PostImageDeleteService;
import com.tavemakers.surf.domain.post.service.image.PostImageGetService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 게시글 삭제 관련 서비스 - Post 도메인 내부 삭제 로직만 담당 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostDeleteService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    private final PostGetService postGetService;
    private final PostImageGetService postImageGetService;
    private final PostImageDeleteService postImageDeleteService;
    private final PostFileGetService postFileGetService;
    private final PostFileDeleteService postFileDeleteService;
    private final MemberGetService memberGetService;

    /** 게시글 삭제 - Post 도메인 내부 데이터만 삭제 (좋아요, 이미지, 게시글) */
    @Transactional
    @LogEvent(value = "post.delete", message = "게시글 삭제 성공")
    public void deletePost(
            @LogParam("post_id") Long postId) {
        Post post = postGetService.getPost(postId);
        Member member = memberGetService.getMember(SecurityUtils.getCurrentMemberId());
        validateOwnerOrManager(post, member);

        // Post 도메인 내부 데이터 삭제
        postLikeRepository.deleteByPostId(postId);

        List<PostImageUrl> postImageUrls = postImageGetService.getPostImageUrls(post.getId());
        if (postImageUrls != null && !postImageUrls.isEmpty()) {
            postImageDeleteService.deleteAll(postImageUrls);
        }

        List<PostFileUrl> postFileUrls = postFileGetService.getPostFileUrls(post.getId());
        if (postFileUrls != null && !postFileUrls.isEmpty()) {
            postFileDeleteService.deleteAll(postFileUrls);
        }

        postRepository.delete(post);
    }

    /** 게시글 소유자 또는 관리자 권한 검증 */
    private void validateOwnerOrManager(Post post, Member member) {
        if (!member.hasDeleteRole() && !post.isOwner(member.getId())) {
            throw new PostDeleteAccessDeniedException();
        }
    }
}
