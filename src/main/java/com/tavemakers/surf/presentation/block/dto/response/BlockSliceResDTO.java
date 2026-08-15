package com.tavemakers.surf.presentation.block.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

@Schema(description = "내 차단 목록 (Slice)")
public record BlockSliceResDTO(

        @Schema(description = "차단한 회원 목록 (없으면 빈 배열)")
        List<BlockedMemberResDTO> content,

        @Schema(description = "현재 페이지 번호", example = "0")
        int pageNumber,

        @Schema(description = "페이지 크기", example = "20")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext
) {

    public static BlockSliceResDTO from(Slice<BlockedMemberResDTO> slice) {
        return new BlockSliceResDTO(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
