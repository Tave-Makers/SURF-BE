package com.tavemakers.surf.domain.post.domain.service.file;

import com.tavemakers.surf.domain.post.presentation.dto.request.PostFileCreateReqDTO;
import com.tavemakers.surf.domain.post.presentation.dto.response.PostFileResDTO;
import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.post.domain.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.domain.repository.PostFileUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostFileCreateService {

    private final PostFileUrlRepository postFileUrlRepository;

    /** 게시글 첨부파일 일괄 저장 */
    @Transactional
    public List<PostFileResDTO> saveAll(Post post, List<PostFileCreateReqDTO> dto) {
        if (dto == null || dto.isEmpty()) {
            return List.of();
        }

        List<PostFileUrl> filesUrlList = dto.stream()
                .map(f -> PostFileUrl.of(post, f))
                .toList();
        return postFileUrlRepository.saveAll(filesUrlList).stream()
                .map(PostFileResDTO::from)
                .toList();
    }
}
