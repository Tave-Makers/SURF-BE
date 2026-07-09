package com.tavemakers.surf.domain.post.repository;

import com.tavemakers.surf.domain.post.dto.PostViewUpdateDto;

import java.util.List;

public interface PostJdbcRepository {
    void viewCountBulkUpdate(List<PostViewUpdateDto> updateDtoList);
}
