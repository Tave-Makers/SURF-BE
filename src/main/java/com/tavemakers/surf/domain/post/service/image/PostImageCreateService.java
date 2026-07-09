package com.tavemakers.surf.domain.post.service.image;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.repository.PostImageUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostImageCreateService {

    private final PostImageUrlRepository postImageUrlRepository;

    /** 게시글 이미지 생성 입력 (표현형 DTO를 도메인에 노출하지 않기 위한 도메인 입력 타입) */
    public record ImageData(String originalUrl, Integer sequence) {}

    /** 게시글 이미지 일괄 저장 (트랜잭션 경계는 호출자 usecase가 소유) */
    public List<PostImageUrl> saveAll(Post post, List<ImageData> imageDataList) {
        if (imageDataList == null || imageDataList.isEmpty()) {
            return List.of();
        }

        List<PostImageUrl> imageUrlList = imageDataList.stream()
                .map(data -> PostImageUrl.of(post, data.originalUrl(), data.sequence()))
                .toList();
        return postImageUrlRepository.saveAll(imageUrlList);
    }

}
