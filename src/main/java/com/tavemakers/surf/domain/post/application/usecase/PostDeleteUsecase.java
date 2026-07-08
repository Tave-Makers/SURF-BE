package com.tavemakers.surf.domain.post.application.usecase;

import com.tavemakers.surf.domain.comment.domain.service.CommentDeleteService;
import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.post.domain.repository.PostRepository;
import com.tavemakers.surf.domain.post.domain.service.post.PostDeleteService;
import com.tavemakers.surf.domain.post.application.query.PostGetService;
import com.tavemakers.surf.domain.reservation.domain.repository.ReservationRepository;
import com.tavemakers.surf.domain.schedule.domain.service.ScheduleDeleteService;
import com.tavemakers.surf.domain.scrap.application.query.ScrapGetService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 삭제 유즈케이스 - 연관 데이터 삭제 조합 */
@Service
@RequiredArgsConstructor
public class PostDeleteUsecase {

    private final PostDeleteService postDeleteService;
    private final PostGetService postGetService;
    private final PostRepository postRepository;
    private final CommentDeleteService commentDeleteService;
    private final ScheduleDeleteService scheduleDeleteService;
    private final ScrapGetService scrapGetService;
    private final ReservationRepository reservationRepository;

    /** 게시글 및 연관 데이터 삭제 */
    @Transactional
    public void deletePost(Long postId) {
        Post post = postGetService.getPost(postId);

        // 연관 데이터 먼저 삭제
        scheduleDeleteService.deleteByPost(post);
        reservationRepository.deleteByPostId(postId);
        scrapGetService.deleteByPostId(postId);
        commentDeleteService.deleteAllByPostId(postId);

        // 게시글 삭제 (권한 검증 및 이미지/좋아요 삭제 포함)
        postDeleteService.deletePost(postId);
    }

    /** 권한 검증 없이 게시글 강제 삭제 — dismiss 전용 */
    @Transactional
    public void forceDeletePost(Post post) {
        scheduleDeleteService.deleteByPost(post);
        reservationRepository.deleteByPostId(post.getId());
        scrapGetService.deleteByPostId(post.getId());
        commentDeleteService.deleteAllByPostId(post.getId());
        postDeleteService.forceDeletePost(post);
    }

    /** 회원 소유 게시글 전체 강제 삭제 — dismiss 전용. 삭제된 게시글 ID 집합을 반환한다 */
    @Transactional
    public Set<Long> deleteAllOwnedBy(Long memberId) {
        List<Post> posts = postRepository.findAllByMemberId(memberId);
        for (Post post : posts) {
            forceDeletePost(post);
        }
        return posts.stream().map(Post::getId).collect(Collectors.toSet());
    }
}
