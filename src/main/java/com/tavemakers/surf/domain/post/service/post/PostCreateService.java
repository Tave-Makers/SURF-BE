package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.exception.CategoryRequiredException;
import com.tavemakers.surf.domain.board.exception.InvalidCategoryMappingException;
import com.tavemakers.surf.domain.board.service.BoardCategoryGetService;
import com.tavemakers.surf.domain.board.service.BoardGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.dto.request.PostCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.request.PostImageCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.post.dto.response.PostImageResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.BoardWriteNotAllowedException;
import com.tavemakers.surf.domain.post.exception.PostImageListEmptyException;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.image.PostImageCreateService;
import com.tavemakers.surf.domain.post.service.support.PostPublishedEvent;
import com.tavemakers.surf.global.logging.LogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 게시글 생성 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCreateService {

    private final PostRepository postRepository;

    private final BoardGetService boardGetService;
    private final BoardCategoryGetService boardCategoryGetService;
    private final MemberGetService memberGetService;
    private final PostImageCreateService imageCreateService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 게시글 생성 및 저장 (예약 처리는 Usecase에서 담당)
     */
    @Transactional
    @LogEvent(value = "post.create", message = "게시글 생성 성공")
    public PostDetailResDTO createPost(PostCreateReqDTO req, Long memberId) {
        Board board = boardGetService.getBoard(req.boardId());
        Member writer = memberGetService.getMember(memberId);

        // BoardType.NOTICE인 경우 관리자인지 검증
        validateWritePermission(board, writer);

        BoardCategory category = resolveCategory(board, req.categoryId());

        Post post = Post.of(req, board, category, writer);
        Post saved = postRepository.save(post);

        if (!req.isReserved()) {
            eventPublisher.publishEvent(new PostPublishedEvent(saved.getId()));
        }

        List<PostImageResDTO> imageUrlResponseList = null;
        if (req.hasImage()) {
            List<PostImageCreateReqDTO> imageUrlList = req.imageUrlList();
            saved.addThumbnailUrl(findFirstImage(imageUrlList));
            imageUrlResponseList = imageCreateService.saveAll(saved, imageUrlList);
        }

        LocalDateTime reservedAt = req.isReserved() ? req.reservedAt() : null;
        return PostDetailResDTO.of(saved, false, false, true, imageUrlResponseList, reservedAt, 0);
    }

    /**
     * 이미지 목록에서 첫 번째 이미지 URL 추출
     */
    private String findFirstImage(List<PostImageCreateReqDTO> dto) {
        if (dto == null || dto.isEmpty()) {
            throw new PostImageListEmptyException();
        }

        PostImageCreateReqDTO postImageCreateReqDTO = dto.stream()
                .min(Comparator.comparing(PostImageCreateReqDTO::sequence))
                .orElse(dto.get(0));
        return postImageCreateReqDTO.originalUrl();
    }

    /**
     * 카테고리 유효성 검증 및 조회
     */
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

    private void validateWritePermission(Board board, Member writer) {
        if (board.isNotice() && !writer.hasDeleteRole()) {
            throw new BoardWriteNotAllowedException();
        }
    }
}
