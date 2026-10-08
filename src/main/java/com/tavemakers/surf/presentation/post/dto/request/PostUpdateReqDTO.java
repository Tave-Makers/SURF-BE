package com.tavemakers.surf.presentation.post.dto.request;

import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;

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

        @Schema(description = "false이면 일정 태그 해제, 생략하면 기존 연결 유지", example = "false")
        Boolean hasSchedule,

        @Schema(description = "연결·교체할 기존 일정 ID (생략하면 기존 연결 유지)", example = "5")
        @Positive Long scheduleId

) implements LogPropsProvider {

        @AssertTrue(message = "일정 연결 시 scheduleId가 필요하며, 연결 해제 시 scheduleId를 보낼 수 없습니다.")
        public boolean isScheduleTagValid() {
                return !(Boolean.TRUE.equals(hasSchedule) && scheduleId == null)
                        && !(Boolean.FALSE.equals(hasSchedule) && scheduleId != null);
        }

        /**
         * title·content만 마스킹된 값으로 교체한 사본.
         * 나머지 필드는 그대로 복사하며, 부분 수정 계약상 null은 null로 보존된다.
         */
        public PostUpdateReqDTO withMaskedText(String maskedTitle, String maskedContent) {
                return new PostUpdateReqDTO(
                        maskedTitle, maskedContent, categoryId, pinned,
                        isReservationChanged, reservedAt,
                        isImageChanged, imageUrlList, isFileChanged, fileList, hasSchedule, scheduleId);
        }

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