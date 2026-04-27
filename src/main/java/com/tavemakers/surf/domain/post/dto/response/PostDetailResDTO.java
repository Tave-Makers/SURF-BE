package com.tavemakers.surf.domain.post.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PostDetailResDTO(
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

        @Schema(description = "게시글 작성자 회원 ID (탈퇴 회원이면 null)", example = "7")
        Long memberId,

        @Schema(description = "게시글 작성자 닉네임", example = "홍길동")
        String nickname,

        @Schema(description = "게시글 작성자 썸네일 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl,

        @Schema(description = "내 게시글 여부", example = "true")
        boolean isMine,

        @Schema(description = "게시글 이미지 링크")
        List<PostImageResDTO> imageUrlList,

        @Schema(description = "게시글 첨부파일 목록")
        List<PostFileResDTO> fileList,

        @Schema(description = "예약된 게시글 여부", example = "true")
        boolean isReserved,

        @Schema(description = "게시글 예약 시간", example = "2023-12-01T10:00:00")
        LocalDateTime reservedAt,

        @Schema(description = "게시글 조회수", example = "100")
        int viewCount,

        @Schema(description = "일정 매핑 유무", example = "true")
        Boolean hasSchedule,

        @Schema(description = "일정 Id", example = "2")
        Long scheduleId
) {
    public static PostDetailResDTO of(
            Post post,
            boolean scrappedByMe,
            boolean likedByMe,
            boolean isMine,
            List<PostImageResDTO> imageUrlList,
            List<PostFileResDTO> fileList,
            LocalDateTime reservedAt,
            int viewCount
    ){
            Member writer = post.getMember();

            return PostDetailResDTO.builder()
                    .postId(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .pinned(post.isPinned())
                    .postedAt(post.getPostedAt())
                    .boardId(post.getBoard().getId())
                    .categoryId(post.getCategory().getId())
                    .scrappedByMe(scrappedByMe)
                    .scrapCount(post.getScrapCount())
                    .likedByMe(likedByMe)
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .memberId(writer.getStatus() == MemberStatus.WITHDRAWN ? null : writer.getId())
                    .nickname(writer.getName())
                    .profileImageUrl(writer.getProfileImageUrl())
                    .isMine(isMine)
                    .imageUrlList(imageUrlList)
                    .fileList(fileList)
                    .isReserved(post.isReserved())
                    .reservedAt(reservedAt)
                    .viewCount(viewCount)
                    .hasSchedule(post.getHasSchedule())
                    .scheduleId(post.getScheduleId())
                    .build();
    }
}
