package com.tavemakers.surf.domain.post.domain.repository;

import com.tavemakers.surf.domain.post.domain.dto.PostViewUpdateDto;

import java.util.List;

public interface PostJdbcRepository {
    void viewCountBulkUpdate(List<PostViewUpdateDto> updateDtoList);
}
