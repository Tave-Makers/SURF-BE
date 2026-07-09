package com.tavemakers.surf.domain.post.repository;

import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageUrlRepository extends JpaRepository<PostImageUrl, Long> {
    List<PostImageUrl> findByPostId(Long postId);
}
