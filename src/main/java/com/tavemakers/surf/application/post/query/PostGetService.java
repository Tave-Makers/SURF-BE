package com.tavemakers.surf.application.post.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.presentation.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.presentation.post.dto.response.PostFileResDTO;
import com.tavemakers.surf.presentation.post.dto.response.PostImageResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.PostNotFoundException;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.application.post.query.PostFileGetService;
import com.tavemakers.surf.application.post.query.PostImageGetService;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.service.support.ViewCountService;
import com.tavemakers.surf.domain.reservation.entity.Reservation;
import com.tavemakers.surf.application.reservation.query.ReservationGetService;
import com.tavemakers.surf.application.scrap.query.ScrapGetService;
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

    private final BlockGetService blockGetService;
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

    /** 게시글 행 잠금 조회 (없으면 Optional.empty) — 삭제된 게시글을 호출자가 직접 처리할 때 사용 */
    public Optional<Post> findPostForUpdate(Long id) {
        return postRepository.findByIdForUpdate(id);
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

    /**
     * 사용자에게 보여도 되는 게시글인지 검증 — 없거나 작성자를 차단했으면 404.
     *
     * <p>댓글 목록처럼 게시글에 딸린 사용자 조회가 앞단에서 호출한다.
     * 차단 사실을 상대에게 노출하지 않기 위해 403이 아니라 404로 존재 자체를 숨긴다.
     *
     * <p>scheduler/event가 쓰는 {@link #getPost}·{@link #readPost}·{@link #findPostById}에는
     * 이 필터를 적용하지 않는다 — 내부 조합까지 차단으로 가리면 예약 발행·알림이 깨진다.
     */
    @Transactional(readOnly = true)
    public void validateVisiblePost(Long postId, Long viewerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        validateNotBlockedAuthor(post, viewerId);
    }

    /** 게시글 상세 조회 (DTO 반환) */
    @Transactional
    public PostDetailResDTO getPostDetail(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        // 차단 가드는 조회수 증가·스크랩·좋아요·첨부 조회보다 먼저 둔다.
        // 뒤로 밀리면 숨겨야 할 글의 조회수가 오르고 부수효과가 남는다.
        validateNotBlockedAuthor(post, memberId);
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

    /** 내가 차단한 작성자의 글이면 존재하지 않는 것으로 취급한다 */
    private void validateNotBlockedAuthor(Post post, Long viewerId) {
        if (blockGetService.isBlockedByMe(viewerId, post.getMember().getId())) {
            throw new PostNotFoundException();
        }
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
