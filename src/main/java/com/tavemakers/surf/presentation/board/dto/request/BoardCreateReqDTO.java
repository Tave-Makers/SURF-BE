package com.tavemakers.surf.presentation.board.dto.request;

import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "게시판 생성 요청 DTO")
public record BoardCreateReqDTO(

        @Schema(description = "게시판 이름", example = "공지사항")
        @NotBlank String name,

        @Schema(description = "게시판 타입", example = "NOTICE")
        @NotNull BoardType type
) implements LogPropsProvider {

        @Override
        public Map<String, Object> buildProps() {
                return Map.of(
                        "title_length", name != null ? name.length() : 0
                );
        }
}