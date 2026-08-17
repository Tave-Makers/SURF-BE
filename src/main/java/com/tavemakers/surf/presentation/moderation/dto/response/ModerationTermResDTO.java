package com.tavemakers.surf.presentation.moderation.dto.response;

import com.tavemakers.surf.domain.moderation.entity.ModerationTerm;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;

import java.time.LocalDateTime;

public record ModerationTermResDTO(
        Long termId,
        ModerationTermType type,
        String text,
        LocalDateTime createdAt
) {

    public static ModerationTermResDTO from(ModerationTerm term) {
        return new ModerationTermResDTO(
                term.getId(),
                term.getType(),
                term.getText(),
                term.getCreatedAt()
        );
    }

}
