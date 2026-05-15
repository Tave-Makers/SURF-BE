package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.board.exception.CategoryRequiredException;
import com.tavemakers.surf.domain.board.exception.InvalidCategoryMappingException;
import com.tavemakers.surf.domain.board.service.BoardCategoryGetService;
import com.tavemakers.surf.domain.board.service.BoardGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.dto.response.PostResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.support.FlagsMapper;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventContext;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 목록 조회 관련 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostListService {

    private final PostRepository postRepository;

    private final MemberGetService memberGetService;
    private final BoardGetService boardGetService;
    private final BoardCategoryGetService boardCategoryGetService;
    private final FlagsMapper flagsMapper;

    /** 내가 작성한 게시글 목록 조회 */
    public Slice<PostResDTO> getMyPosts(Long myId, Pageable pageable) {
        memberGetService.validateMember(myId);
        Slice<Post> slice = postRepository.findByMemberId(myId, pageable);
        FlagsMapper.Flags flags = flagsMapper.resolveFlags(myId, slice.getContent());
        return slice.map(p -> flagsMapper.toRes(p, flags));
    }

    /** 특정 작성자의 게시글 목록 조회 */
    @LogEvent(value = "post.by.author.list", message = "특정 작성자 게시글 목록 조회")
    public Slice<PostResDTO> getPostsByMember(
            @LogParam(value = "author_id") Long authorId, Long viewerId, Pageable pageable) {
        memberGetService.validateMember(authorId);
        Slice<Post> slice = postRepository.findByMemberId(authorId, pageable);
        LogEventContext.put("count", slice.getNumberOfElements());
        FlagsMapper.Flags flags = flagsMapper.resolveFlags(viewerId, slice.getContent());
        return slice.map(p -> flagsMapper.toRes(p, flags));
    }

    /** 게시판 및 카테고리별 게시글 목록 조회 (slug 기반) */
    @LogEvent(value = "post.list.view", message = "게시판 리스트 화면 진입")
    public Slice<PostResDTO> getPostsByBoardAndCategory(
            Long boardId,
            String categorySlug,
            Long viewerId,
            Pageable pageable
    ) {
        Board board = boardGetService.getBoard(boardId);

        Member viewer = memberGetService.getMember(viewerId);
        boolean isManager = viewer.hasDeleteRole();

        boolean all = (categorySlug == null || categorySlug.isBlank() || "all".equalsIgnoreCase(categorySlug));
        String categoryForLog = all ? "all" : categorySlug;

        boolean isNotice = board.getType() == BoardType.NOTICE;

        Slice<Post> slice;
        if (all) {
            slice = isManager
                    ? postRepository.findByBoardId(boardId, pageable)
                    : postRepository.findByBoardIdAndIsReservedFalse(boardId, pageable);
        } else {
            BoardCategory category = boardCategoryGetService.getCategoryByBoardAndSlug(boardId, categorySlug);

            slice = isManager
                    ? postRepository.findByBoardIdAndCategoryId(boardId, category.getId(), pageable)
                    : postRepository.findByBoardIdAndCategoryIdAndIsReservedFalse(boardId, category.getId(), pageable);
        }

        if (isNotice) {
            LogEventContext.overrideEvent("notice_list_view");
            LogEventContext.overrideMessage("공지 리스트 화면 진입");
            LogEventContext.put("category", "notice");
        } else {
            LogEventContext.overrideEvent("post_list_view");
            LogEventContext.overrideMessage("게시판 리스트 화면 진입");

            LogEventContext.put("board_id", boardId);
            LogEventContext.put("category", categoryForLog);
            LogEventContext.put("loaded_count", slice.getNumberOfElements());
        }

        FlagsMapper.Flags flags = flagsMapper.resolveFlags(viewerId, slice.getContent());
        return slice.map(p -> flagsMapper.toRes(p, flags));
    }

    /** 게시판 및 카테고리별 게시글 목록 조회 (ID 기반) */
    public Slice<PostResDTO> getPostsByBoardAndCategory(Long boardId, Long categoryId, Long viewerId, Pageable pageable) {
        Board board = boardGetService.getBoard(boardId);
        resolveCategory(board, categoryId);
        Slice<Post> slice = postRepository.findByBoardIdAndCategoryId(boardId, categoryId, pageable);
        FlagsMapper.Flags flags = flagsMapper.resolveFlags(viewerId, slice.getContent());
        return slice.map(p -> flagsMapper.toRes(p, flags));
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
}
