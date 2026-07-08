package com.tavemakers.surf.domain.post.application.query;

import com.tavemakers.surf.domain.post.presentation.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.post.presentation.dto.response.PostFileResDTO;
import com.tavemakers.surf.domain.post.presentation.dto.response.PostImageResDTO;
import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.post.domain.exception.PostNotFoundException;
import com.tavemakers.surf.domain.post.domain.repository.PostRepository;
import com.tavemakers.surf.domain.post.application.query.PostFileGetService;
import com.tavemakers.surf.domain.post.application.query.PostImageGetService;
import com.tavemakers.surf.domain.post.domain.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.domain.service.support.ViewCountService;
import com.tavemakers.surf.domain.reservation.domain.entity.Reservation;
import com.tavemakers.surf.domain.reservation.application.query.ReservationGetService;
import com.tavemakers.surf.domain.scrap.application.query.ScrapGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final PostFileGetService fileGetService;
    private final ViewCountService viewCountService;
    private final ReservationGetService reservationGetService;

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

    /** 게시글 행 잠금 조회 — postId 단위 직렬화가 필요한 작업(예약 변경)용. 호출자 트랜잭션에서 잠금 유지 */
    public Post getPostForUpdate(Long id) {
        return postRepository.findByIdForUpdate(id)
                .orElseThrow(PostNotFoundException::new);
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

    /** 특정 카테고리에 속한 게시글 존재 여부 */
    @Transactional(readOnly = true)
    public boolean existsByCategory(Long categoryId) {
        return postRepository.existsByCategoryId(categoryId);
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
        List<PostFileResDTO> fileUrlList = getPostFileList(post);
        int viewCount = viewCountService.increaseViewCount(post, memberId);
        LocalDateTime reservedAt = null;
        if (post.isReserved()) {
            Reservation reservation = reservationGetService.findByPostIdAndStatus(postId);
            if (reservation != null) {
                reservedAt = LocalDateTime.ofInstant(
                        reservation.getReservedAt(),
                        ZoneId.of("Asia/Seoul")
                );
            }
        }

        return PostDetailResDTO.of(post, scrappedByMe, likedByMe, isMine, imageUrlList, fileUrlList, reservedAt, viewCount);
    }

    /** 스크랩 수 원자적 증가 */
    public void increaseScrapCount(Long postId) {
        postRepository.increaseScrapCount(postId);
    }

    /** 스크랩 수 원자적 감소 */
    public void decreaseScrapCount(Long postId) {
        postRepository.decreaseScrapCount(postId);
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

    public List<PostImageResDTO> getImageUrlList(Post post) {
        return imageGetService.getPostImageUrls(post.getId()).stream()
                .map(PostImageResDTO::from)
                .sorted(Comparator.comparing(PostImageResDTO::sequence))
                .toList();
    }

    public List<PostFileResDTO> getPostFileList(Post post) {
        return fileGetService.getPostFileUrls(post.getId()).stream()
                .map(PostFileResDTO::from)
                .sorted(Comparator.comparing(PostFileResDTO::sequence))
                .toList();
    }
}
