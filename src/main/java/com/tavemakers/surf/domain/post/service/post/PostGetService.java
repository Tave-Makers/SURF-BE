package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.post.dto.response.PostImageResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.PostNotFoundException;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.image.PostImageGetService;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.service.support.ViewCountService;
import com.tavemakers.surf.domain.reservation.usecase.ReservationUsecase;
import com.tavemakers.surf.domain.scrap.service.ScrapGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostGetService {

    private final PostRepository postRepository;

    private final ScrapGetService scrapGetService;
    private final PostLikeService postLikeService;
    private final PostImageGetService imageGetService;
    private final ViewCountService viewCountService;
    private final ReservationUsecase reservationUsecase;

    /** 게시글 ID로 엔티티 조회 (없으면 예외 발생) */
    @Transactional
    public Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(PostNotFoundException::new);
    }

    /** 게시글 ID로 엔티티 조회 (없으면 null 반환) */
    public Post getPostOrNull(Long id) {
        return postRepository.findById(id)
                .orElse(null);
    }

    /** 게시글 읽기 전용 조회 */
    @Transactional(readOnly = true)
    public Post readPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(PostNotFoundException::new);
    }

    /** 게시글 존재 여부 검증 */
    @Transactional(readOnly = true)
    public void validatePost(Long id) {
        postRepository.findById(id)
                .orElseThrow(PostNotFoundException::new);
    }

    /** 게시글 상세 조회 (DTO 반환) */
    @Transactional
    public PostDetailResDTO getPostDetail(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        boolean scrappedByMe = scrapGetService.isScrappedByMe(memberId, postId);
        boolean likedByMe = postLikeService.isLikedByMe(memberId, postId);
        boolean isMine = post.isOwner(memberId);
        List<PostImageResDTO> imageUrlList = getImageUrlList(post);
        int viewCount = viewCountService.increaseViewCount(post, memberId);
        LocalDateTime reservedAt = null;
        if (post.isReserved()) {
            reservedAt = reservationUsecase.getReservedAt(postId);
        }

        return PostDetailResDTO.of(post, scrappedByMe, likedByMe, isMine, imageUrlList, reservedAt, viewCount);
    }

    /** 게시글 예약을 위한 Post 조회 */
    public Optional<Post> findPost(Long id) {
        return postRepository.findById(id);
    }

    /** 게시글 ID로 엔티티 조회 */
    @Transactional(readOnly = true)
    public Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private List<PostImageResDTO> getImageUrlList(Post post) {
        return imageGetService.getPostImageUrls(post.getId()).stream()
                .map(PostImageResDTO::from)
                .sorted(Comparator.comparing(PostImageResDTO::sequence))
                .toList();
    }
}
