package com.tavemakers.surf.domain.post.dto.request;

import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "게시글 생성 요청 DTO")
public record PostCreateReqDTO(

        @Schema(description = "게시판 ID", example = "1")
        @NotNull Long boardId,

        @Schema(description = "세부 카테고리 ID", example = "2")
        @NotNull Long categoryId,

        @Schema(description = "게시글 제목", example = "만남의 장 공지사항")
        @NotBlank String title,

        @Schema(description = "게시글 본문 내용", example = "전반기 만남의 장 언제 어디에 진행합니다!")
        @NotBlank String content,

        @Schema(description = "게시글 상단 고정 여부", example = "true")
        Boolean pinned,

        @Schema(description = "예약 시간", example = "2025-10-29T00:00:00")
        @Future(message = "예약 시간은 현재 이후여야 합니다")
        LocalDateTime reservedAt,

        @Schema(description = "게시글 이미지 목록")
        List<PostImageCreateReqDTO> imageUrlList,

        @Schema(description = "게시글 첨부파일 목록")
        List<PostFileCreateReqDTO> fileList,

        @Schema(description = "일정 매핑 유무", example = "true")
        Boolean hasSchedule

) implements LogPropsProvider {

        @Override
        public Map<String, Object> buildProps() {
                return Map.of(
                        "board_id", boardId,
                        "title_length", title != null ? title.length() : 0
                );
        }

        public boolean isReserved() {
                return reservedAt != null;
        }

        public boolean hasImage() {
                return imageUrlList != null && !imageUrlList.isEmpty();
        }

        public boolean hasFile() {
                return fileList != null && !fileList.isEmpty();
        }

}
