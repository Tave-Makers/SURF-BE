package com.tavemakers.surf.domain.post.service.image;

import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.repository.PostImageUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 게시글 이미지 삭제 서비스. 트랜잭션 경계는 호출자(usecase)가 소유한다. */
@Service
@RequiredArgsConstructor
public class PostImageDeleteService {

    private final PostImageUrlRepository repository;

    /** 게시글 이미지 일괄 삭제 */
    public void deleteAll(List<PostImageUrl> beforeImages) {
        if (beforeImages == null || beforeImages.isEmpty()) {
            return;
        }
        repository.deleteAllInBatch(beforeImages);
    }

    /** 게시글 이미지 단건 삭제 */
    public void delete(PostImageUrl image) {
        repository.delete(image);
    }

}
