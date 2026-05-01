package com.tavemakers.surf.domain.post.repository;

import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostFileUrlRepository extends JpaRepository<PostFileUrl, Long> {
    List<PostFileUrl> findByPostId(Long postId);
}
