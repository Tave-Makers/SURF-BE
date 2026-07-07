package com.tavemakers.surf.domain.member.presentation.dto.response;

import com.tavemakers.surf.domain.member.domain.entity.Track;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record TrackResDTO(

        @Schema(description = "기수", example = "15")
        Integer generation,

        @Schema(description = "파트 이름", example = "BACKEND")
        String part
) {
    public static TrackResDTO from(Track track) {
        return TrackResDTO.builder()
                .generation(track.getGeneration())
                .part(track.getPart().name())
                .build();
    }
}
