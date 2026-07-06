package com.tavemakers.surf.domain.post.service.support;

import com.tavemakers.surf.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 게시글 댓글 수 증감 — 엔티티 메모리 증감(commentCount++/--)은 동시 요청 시 lost update가
 * 발생하므로 DB 원자적 UPDATE로 처리한다. commentCount는 게시글 삭제·수정과 독립적이라
 * @Version 낙관적 락 없이 원자적 증감만으로 충분하다 (좋아요 카운트와 동일 접근).
 */
@Service
@RequiredArgsConstructor
public class PostCommentCountService {

    private final PostRepository postRepository;

    public void increase(Long postId) {
        postRepository.increaseCommentCount(postId);
    }

    public void decrease(Long postId) {
        postRepository.decreaseCommentCount(postId);
    }
}
