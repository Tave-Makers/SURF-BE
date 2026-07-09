package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.exception.CategoryRequiredException;
import com.tavemakers.surf.domain.board.exception.InvalidCategoryMappingException;
import com.tavemakers.surf.application.board.query.BoardCategoryGetService;
import com.tavemakers.surf.application.board.query.BoardGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.exception.BoardWriteNotAllowedException;
import com.tavemakers.surf.domain.post.exception.PostImageListEmptyException;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.file.PostFileCreateService;
import com.tavemakers.surf.domain.post.service.image.PostImageCreateService;
import com.tavemakers.surf.domain.post.event.PostPublishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 게시글 생성 관련 서비스. DTO를 알지 못하며 원시값·엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(PostCreateUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class PostCreateService {

    private final PostRepository postRepository;

    private final BoardGetService boardGetService;
    private final BoardCategoryGetService boardCategoryGetService;
    private final MemberGetService memberGetService;
    private final PostImageCreateService imageCreateService;
    private final PostFileCreateService fileCreateService;
    private final ApplicationEventPublisher eventPublisher;

    /** 게시글 생성 결과 (표현형 매핑은 usecase가 담당) */
    public record PostCreateResult(Post post, List<PostImageUrl> images, List<PostFileUrl> files) {}

    /**
     * 게시글 생성 및 저장 (예약 처리는 Usecase에서 담당)
     */
    public PostCreateResult createPost(
            String title, String content, Boolean pinned, boolean isReserved, Boolean hasSchedule,
            Long boardId, Long categoryId,
            List<PostImageCreateService.ImageData> imageDataList,
            List<PostFileCreateService.FileData> fileDataList,
            Long memberId) {
        Board board = boardGetService.getBoard(boardId);
        Member writer = memberGetService.getMember(memberId);

        // BoardType.NOTICE인 경우 관리자인지 검증
        validateWritePermission(board, writer);

        BoardCategory category = resolveCategory(board, categoryId);

        Post post = Post.of(title, content, pinned, isReserved, hasSchedule, board, category, writer);
        Post saved = postRepository.save(post);

        if (!isReserved) {
            eventPublisher.publishEvent(new PostPublishedEvent(saved.getId()));
        }

        List<PostImageUrl> images = List.of();
        if (imageDataList != null && !imageDataList.isEmpty()) {
            saved.addThumbnailUrl(findFirstImage(imageDataList));
            images = imageCreateService.saveAll(saved, imageDataList);
        }

        List<PostFileUrl> files = List.of();
        if (fileDataList != null && !fileDataList.isEmpty()) {
            files = fileCreateService.saveAll(saved, fileDataList);
        }

        return new PostCreateResult(saved, images, files);
    }

    /**
     * 이미지 목록에서 첫 번째 이미지 URL 추출
     */
    private String findFirstImage(List<PostImageCreateService.ImageData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            throw new PostImageListEmptyException();
        }

        PostImageCreateService.ImageData first = dataList.stream()
                .min(Comparator.comparing(PostImageCreateService.ImageData::sequence))
                .orElse(dataList.get(0));
        return first.originalUrl();
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
