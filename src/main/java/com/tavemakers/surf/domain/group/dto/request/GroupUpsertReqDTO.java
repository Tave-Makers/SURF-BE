package com.tavemakers.surf.domain.group.dto.request;

import com.tavemakers.surf.domain.group.entity.GroupType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


public record GroupUpsertReqDTO(
        @NotNull
        Integer generation,

        @NotNull
        GroupType type,

        @NotBlank
        @Size(max = 50)
        String name,

        @NotBlank
        @Size(max = 500)
        String description,

        @NotNull
        Long leaderMemberId,

        @NotEmpty
        List<@NotNull Long> memberIds
) {}