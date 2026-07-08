package com.tavemakers.surf.domain.post.application.usecase;

import com.tavemakers.surf.domain.post.presentation.dto.response.PostLikeListResDTO;
import com.tavemakers.surf.domain.post.application.query.PostLikeGetService;
import com.tavemakers.surf.domain.post.application.query.PostGetService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostUsecase {
    private final PostGetService postGetService;
    private final PostLikeGetService postLikeGetService;

    /** 게시글 좋아요 목록 조회 */
    @Transactional(readOnly = true)
    @LogEvent("post.like.list.view")
    public PostLikeListResDTO getPostLikes (@LogParam("post_id") Long postId) {
        postGetService.validatePost(postId);
        return postLikeGetService.getPostLikes(postId);
    }
}
