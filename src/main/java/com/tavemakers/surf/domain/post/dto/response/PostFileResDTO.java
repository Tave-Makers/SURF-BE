package com.tavemakers.surf.domain.post.dto.response;

import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PostFileResDTO(

        @Schema(description = "파일 ID")
        Long fileId,

        @Schema(description = "파일 S3 URL")
        String fileUrl,

        @Schema(description = "원본 파일명")
        String originalFileName,

        @Schema(description = "파일 순서")
        Integer sequence
) {
    public static PostFileResDTO from(PostFileUrl fileURL){
        return PostFileResDTO.builder()
                .fileId(fileURL.getId())
                .fileUrl(fileURL.getFileUrl())
                .originalFileName(fileURL.getOriginalFileName())
                .sequence(fileURL.getSequence())
                .build();
    }
}
