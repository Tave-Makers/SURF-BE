package com.tavemakers.surf.domain.post.dto.request;

import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "게시글 수정 요청 DTO")
public record PostUpdateReqDTO(

        @Schema(description = "게시글 제목 (null이면 기존 값 유지, 공백만으로는 불가)", example = "만남의 장 공지사항")
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "제목은 공백만으로 구성될 수 없습니다.")
        String title,

        @Schema(description = "게시글 본문 내용 (null이면 기존 값 유지, 공백만으로는 불가)", example = "전반기 만남의 장 언제 어디에 진행합니다!")
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "본문 내용은 공백만으로 구성될 수 없습니다.")
        String content,

        @Schema(description = "세부 카테고리 ID", example = "2")
        Long categoryId,

        @Schema(description = "게시글 상단 고정 여부", example = "true")
        Boolean pinned,

        @Schema(description = "게시 예약 시간 변경 여부")
        Boolean isReservationChanged,

        @Schema(description = "변경된 예약 시간")
        LocalDateTime reservedAt,

        @Schema(description = "이미지 변경 여부", example = "true")
        Boolean isImageChanged,

        @Schema(description = "게시글 이미지")
        List<PostImageCreateReqDTO> imageUrlList,

        @Schema(description = "파일 변경 여부", example = "true")
        Boolean isFileChanged,

        @Schema(description = "게시글 첨부파일")
        List<PostFileCreateReqDTO> fileList,

        @Schema(description = "일정 매핑 유무", example = "true")
        Boolean hasSchedule

) implements LogPropsProvider {

        @Override
        public Map<String, Object> buildProps() {
                boolean contentChanged = content != null && !content.isBlank();

                List<String> changedFields = new ArrayList<>();
                if (title != null && !title.isBlank()) changedFields.add("title");
                if (contentChanged) changedFields.add("content");
                if (pinned != null) changedFields.add("pinned");
                if (isImageChanged != null) changedFields.add("has_image_changed");

                return Map.of(
                        "changed_fields", changedFields,
                        "edit_length", contentChanged ? String.valueOf(content.length()) : "notChanged"
                );
        }

}