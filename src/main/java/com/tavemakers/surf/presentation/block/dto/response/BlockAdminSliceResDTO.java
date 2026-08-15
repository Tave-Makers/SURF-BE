package com.tavemakers.surf.presentation.block.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

@Schema(description = "관리자 차단 관계 목록 (Slice)")
public record BlockAdminSliceResDTO(

        @Schema(description = "차단 관계 목록 (없으면 빈 배열)")
        List<BlockAdminResDTO> content,

        @Schema(description = "현재 페이지 번호", example = "0")
        int pageNumber,

        @Schema(description = "페이지 크기", example = "20")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext
) {

    public static BlockAdminSliceResDTO from(Slice<BlockAdminResDTO> slice) {
        return new BlockAdminSliceResDTO(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
