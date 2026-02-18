package com.tavemakers.surf.domain.team.dto.request;

import com.tavemakers.surf.domain.team.entity.TeamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


public record TeamUpsertReqDTO(
        @NotNull
        Integer generation,

        @NotNull
        TeamType type,

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