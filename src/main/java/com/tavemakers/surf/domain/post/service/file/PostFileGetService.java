package com.tavemakers.surf.domain.post.service.file;

import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.repository.PostFileUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostFileGetService {

    private final PostFileUrlRepository repository;

    /** 게시글 파일 URL 목록 조회 */
    public List<PostFileUrl> getPostFileUrls(Long postId) {
        return repository.findByPostId(postId);
    }
}
