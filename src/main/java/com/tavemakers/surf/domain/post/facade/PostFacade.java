package com.tavemakers.surf.domain.post.facade;

import com.tavemakers.surf.domain.post.dto.res.PostLikeListResDTO;
import com.tavemakers.surf.domain.post.service.PostGetService;
import com.tavemakers.surf.domain.post.service.PostLikeGetService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PostFacade {
    private final PostGetService postGetService;
    private final PostLikeGetService postLikeGetService;

    @Transactional(readOnly = true)
    @LogEvent("post.like.list.view")
    public PostLikeListResDTO getPostLikes (@LogParam("post_id") Long postId) {
        postGetService.validatePost(postId);
        return postLikeGetService.getPostLikes(postId);
    }
}
