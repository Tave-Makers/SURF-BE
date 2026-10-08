package com.tavemakers.surf.presentation.schedule.dto.response;

import com.tavemakers.surf.domain.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일정에 연결된 공지 바로가기")
public record SchedulePostResDTO(
        @Schema(description = "공지 ID", example = "12") Long postId,
        @Schema(description = "공지 제목", example = "만남의 장 추가 안내") String title
) {
    public static SchedulePostResDTO from(Post post) {
        return new SchedulePostResDTO(post.getId(), post.getTitle());
    }
}
