package com.tavemakers.surf.domain.post.service.image;

import com.tavemakers.surf.domain.post.dto.request.PostImageCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.response.PostImageResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.repository.PostImageUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostImageCreateService {

    private final PostImageUrlRepository postImageUrlRepository;

    /** 게시글 이미지 일괄 저장 */
    @Transactional
    public List<PostImageResDTO> saveAll(Post post, List<PostImageCreateReqDTO> dto) {
        if (dto == null || dto.isEmpty()) {
            return List.of();
        }

        List<PostImageUrl> imageUrlList = dto.stream()
                .map(url -> PostImageUrl.of(post, url))
                .toList();
        return postImageUrlRepository.saveAll(imageUrlList)
                .stream()
                .map(PostImageResDTO::from)
                .sorted(Comparator.comparing(PostImageResDTO::sequence))
                .toList();
    }

}
