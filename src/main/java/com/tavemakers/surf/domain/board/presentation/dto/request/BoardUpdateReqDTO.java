package com.tavemakers.surf.domain.board.presentation.dto.request;

import com.tavemakers.surf.domain.board.domain.entity.BoardType;
import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "게시판 수정 요청 DTO")
public record BoardUpdateReqDTO(

        @Schema(description = "게시판 이름", example = "공지사항")
        @NotBlank String name,

        @Schema(description = "게시판 타입", example = "NOTICE")
        @NotNull BoardType type
) implements LogPropsProvider {

        @Override
        public Map<String, Object> buildProps() {
                List<String> changedFields = new ArrayList<>();
                if (name != null && !name.isBlank()) changedFields.add("name");
                if (type != null) changedFields.add("type");

                return Map.of("changed_fields", changedFields);
        }
}