package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.comment.service.CommentDeleteService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.reservation.repository.ReservationRepository;
import com.tavemakers.surf.domain.schedule.service.ScheduleDeleteService;
import com.tavemakers.surf.domain.scrap.service.ScrapGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 삭제 유즈케이스 - 연관 데이터 삭제 조합 */
@Service
@RequiredArgsConstructor
public class PostDeleteUsecase {

    private final PostDeleteService postDeleteService;
    private final PostGetService postGetService;
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
}
