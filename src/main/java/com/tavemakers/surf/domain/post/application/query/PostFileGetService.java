package com.tavemakers.surf.domain.post.application.query;

import com.tavemakers.surf.domain.post.domain.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.domain.exception.PostFileNotFoundException;
import com.tavemakers.surf.domain.post.domain.repository.PostFileUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostFileGetService {

    private final PostFileUrlRepository postFileUrlRepository;

    /** 게시글 파일 URL 목록 조회 */
    @Transactional(readOnly = true)
    public List<PostFileUrl> getPostFileUrls(Long postId) {
        return postFileUrlRepository.findByPostId(postId);
    }

    /** 게시글 첨부파일 단건 조회 */
    @Transactional(readOnly = true)
    public PostFileUrl getPostFileUrl(Long fileId) {
        return postFileUrlRepository.findById(fileId)
                .orElseThrow(PostFileNotFoundException::new);
    }
}
