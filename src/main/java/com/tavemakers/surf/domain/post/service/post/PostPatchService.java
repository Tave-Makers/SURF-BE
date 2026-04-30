package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.exception.CategoryRequiredException;
import com.tavemakers.surf.domain.board.exception.InvalidCategoryMappingException;
import com.tavemakers.surf.domain.board.service.BoardCategoryGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.dto.request.PostFileCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.request.PostImageCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.request.PostUpdateReqDTO;
import com.tavemakers.surf.domain.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.post.dto.response.PostFileResDTO;
import com.tavemakers.surf.domain.post.dto.response.PostImageResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.exception.PostDeleteAccessDeniedException;
import com.tavemakers.surf.domain.post.exception.PostImageListEmptyException;
import com.tavemakers.surf.domain.post.exception.PostNotFoundException;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.file.PostFileCreateService;
import com.tavemakers.surf.domain.post.service.file.PostFileDeleteService;
import com.tavemakers.surf.domain.post.service.file.PostFileGetService;
import com.tavemakers.surf.domain.post.service.image.PostImageDeleteService;
import com.tavemakers.surf.domain.post.service.image.PostImageGetService;
import com.tavemakers.surf.domain.post.service.image.PostImageCreateService;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.service.support.ViewCountService;
import com.tavemakers.surf.domain.reservation.entity.Reservation;
import com.tavemakers.surf.domain.reservation.service.ReservationGetService;
import com.tavemakers.surf.domain.scrap.service.ScrapGetService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/** 게시글 수정 관련 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostPatchService {

    private final PostRepository postRepository;

    private final BoardCategoryGetService boardCategoryGetService;
    private final ScrapGetService scrapGetService;
    private final PostGetService postGetService;
    private final PostLikeService postLikeService;
    private final ReservationGetService reservationGetService;
    private final PostImageCreateService imageCreateService;
    private final PostImageGetService imageGetService;
    private final PostImageDeleteService imageDeleteService;
    private final PostFileCreateService fileCreateService;
    private final PostFileGetService fileGetService;
    private final PostFileDeleteService fileDeleteService;
    private final MemberGetService memberGetService;
    private final ViewCountService viewCountService;

    /** 게시글 수정 (예약 변경 처리는 Usecase에서 담당) */
    @Transactional
    @LogEvent(value = "post.update", message = "게시글 수정 성공")
    public PostDetailResDTO updatePost(
            @LogParam("post_id") Long postId,
            PostUpdateReqDTO req, Long viewerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        Member member = memberGetService.getMember(SecurityUtils.getCurrentMemberId());
        validateOwnerOrManager(post, member);

        BoardCategory newCategory = (req.categoryId() != null)
                ? resolveCategory(post.getBoard(), req.categoryId())
                : post.getCategory();

        post.update(req, post.getBoard(), newCategory);

        boolean scrappedByMe = scrapGetService.isScrappedByMe(viewerId, postId);
        boolean likedByMe = postLikeService.isLikedByMe(viewerId, postId);
        int viewCount = viewCountService.increaseViewCount(post, viewerId);

        LocalDateTime reservedAt = null;
        if (post.isReserved()) {
            Reservation reservation = reservationGetService.findByPostIdAndStatus(postId);
            if (reservation != null) {
                reservedAt = LocalDateTime.ofInstant(reservation.getReservedAt(), ZoneId.of("Asia/Seoul"));
            }
        }

        // 이미지 변경
        // TODO Spring Event로 PostImageUrl 삭제 로직 분리.
        List<PostImageResDTO> imageDtoList;
        if (Boolean.TRUE.equals(req.isImageChanged())) {
            deleteExistingImages(post);
            List<PostImageCreateReqDTO> changeImages = req.imageUrlList();
            if (changeImages == null || changeImages.isEmpty()) {
                post.addThumbnailUrl(null);
                imageDtoList = null;
            } else {
                post.addThumbnailUrl(findFirstImage(changeImages));
                imageDtoList = imageCreateService.saveAll(post, changeImages);
            }
        } else {
            imageDtoList = postGetService.getImageUrlList(post);
        }

        // 파일 변경
        List<PostFileResDTO> fileDtoList;
        if (Boolean.TRUE.equals(req.isFileChanged())) {
            deleteExistingFiles(post);
            List<PostFileCreateReqDTO> changeFiles = req.fileList();
            if (changeFiles == null || changeFiles.isEmpty()) {
                fileDtoList = null;
            } else {
                fileDtoList = fileCreateService.saveAll(post, changeFiles);
            }
        } else {
            fileDtoList = postGetService.getPostFileList(post);
        }

        return PostDetailResDTO.of(post, scrappedByMe, likedByMe, true, imageDtoList, fileDtoList, reservedAt, viewCount);
    }

    /** 기존 이미지 삭제 */
    private void deleteExistingImages(Post post) {
        List<PostImageUrl> beforeImages = imageGetService.getPostImageUrls(post.getId());
        imageDeleteService.deleteAll(beforeImages);
    }

    /** 기존 첨부파일 삭제 */
    private void deleteExistingFiles(Post post) {
        List<PostFileUrl> beforeFiles = fileGetService.getPostFileUrls(post.getId());
        fileDeleteService.deleteAll(beforeFiles);
    }

    /** 카테고리 유효성 검증 및 조회 */
    private BoardCategory resolveCategory(Board board, Long categoryId) {
        if (categoryId == null) {
            throw new CategoryRequiredException();
        }
        BoardCategory category = boardCategoryGetService.getCategory(categoryId);
        if (!category.getBoard().getId().equals(board.getId())) {
            throw new InvalidCategoryMappingException();
        }
        return category;
    }

    /** 이미지 목록에서 첫 번째 이미지 URL 추출 */
    private String findFirstImage(List<PostImageCreateReqDTO> dto) {
        if (dto == null || dto.isEmpty()) {
            throw new PostImageListEmptyException();
        }

        PostImageCreateReqDTO postImageCreateReqDTO = dto.stream()
                .min(Comparator.comparing(PostImageCreateReqDTO::sequence))
                .orElse(dto.get(0));
        return postImageCreateReqDTO.originalUrl();
    }

    /** 게시글 소유자 또는 관리자 권한 검증 */
    private void validateOwnerOrManager(Post post, Member member) {
        if (!member.hasDeleteRole() && !post.isOwner(member.getId())) {
            throw new PostDeleteAccessDeniedException();
        }
    }
}
