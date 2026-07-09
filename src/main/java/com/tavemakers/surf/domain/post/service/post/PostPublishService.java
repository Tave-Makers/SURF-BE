package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.PostNotFoundException;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 예약 게시글 발행 커맨드 — 발행 상태 전이는 게시글 행 락 아래에서만 수행한다.
 * 트랜잭션은 호출자(application 계층)가 연다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostPublishService {

    private final PostRepository postRepository;

    /**
     * 예약 게시글 발행. 행 락(findByIdForUpdate)으로 예약 변경 트랜잭션과 직렬화하고,
     * 이미 발행된 게시글이면 발행하지 않는다 (스케줄러 중복 실행 멱등 no-op).
     *
     * @return 발행했으면 true, 이미 발행 상태라 건너뛰었으면 false
     */
    public boolean publishReservedPost(Long postId) {
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(PostNotFoundException::new);
        if (!post.isReserved()) {
            log.info("이미 발행된 게시글이므로 발행을 건너뜁니다. postId={}", postId);
            return false;
        }
        post.publish();
        return true;
    }

}
