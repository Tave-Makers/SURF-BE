package com.tavemakers.surf.domain.post.service.image;

import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.repository.PostImageUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostImageGetService {

    private final PostImageUrlRepository postImageUrlRepository;

    /** 게시글 이미지 URL 목록 조회 */
    @Transactional(readOnly = true)
    public List<PostImageUrl> getPostImageUrls(Long postId) {
        return postImageUrlRepository.findByPostId(postId);
    }

}
