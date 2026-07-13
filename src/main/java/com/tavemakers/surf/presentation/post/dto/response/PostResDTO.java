package com.tavemakers.surf.presentation.post.dto.response;

import com.tavemakers.surf.domain.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "게시글 응답 DTO")
@Builder
public record PostResDTO(

        @Schema(description = "게시글 ID", example = "1")
        Long postId,

        @Schema(description = "게시글 제목", example = "만남의 장 공지사항")
        String title,

        @Schema(description = "게시글 본문 내용", example = "전반기 만남의 장 언제 어디에 진행합니다!")
        String content,

        @Schema(description = "게시글 상단 고정 여부", example = "true")
        boolean pinned,

        @Schema(description = "게시글 작성 일시", example = "2023-10-05T14:48:00")
        LocalDateTime postedAt,

        @Schema(description = "게시글 썸네일 이미지 URL")
        String thumbnailImageUrl,

        @Schema(description = "게시판 ID", example = "1")
        Long boardId,

        @Schema(description = "세부 카테고리 ID", example = "2")
        Long categoryId,

        @Schema(description = "내가 스크랩한 게시글인지 여부", example = "true")
        boolean scrappedByMe,

        @Schema(description = "게시글이 스크랩된 수", example = "10")
        long scrapCount,

        @Schema(description = "내가 좋아요한 게시글인지 여부", example = "true")
        boolean likedByMe,

        @Schema(description = "게시글이 좋아요된 수", example = "5")
        long likeCount,

        @Schema(description = "게시글 댓글 수", example = "0")
        long commentCount,

        @Schema(description = "게시글 작성자 닉네임", example = "홍길동")
        String nickname,

        @Schema(description = "예약된 게시글 여부", example = "true")
        boolean isReserved,

        @Schema(description = "게시글 조회수", example = "100")
        int viewCount

) {
    public static PostResDTO from(Post post, boolean scrappedByMe, boolean likedByMe) {
        return PostResDTO.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .pinned(post.isPinned())
                .postedAt(post.getPostedAt())
                .thumbnailImageUrl(post.getThumbnailUrl())
                .boardId(post.getBoard().getId())
                .categoryId(post.getCategory().getId())
                .scrappedByMe(scrappedByMe)
                .scrapCount(post.getScrapCount())
                .likedByMe(likedByMe)
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .nickname(post.getMember().getName())
                .isReserved(post.isReserved())
                .viewCount(post.getViewCount())
                .build();
    }
}
