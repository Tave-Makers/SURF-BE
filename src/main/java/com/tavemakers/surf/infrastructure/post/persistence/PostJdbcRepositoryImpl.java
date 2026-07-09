package com.tavemakers.surf.infrastructure.post.persistence;
import com.tavemakers.surf.domain.post.repository.PostJdbcRepository;

import com.tavemakers.surf.domain.post.dto.PostViewUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostJdbcRepositoryImpl implements PostJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void viewCountBulkUpdate(List<PostViewUpdateDto> updateDtoList) {
        String bulkUpdateSql =
                "UPDATE post " +
                "SET view_count = view_count + ? " +
                "WHERE post_id = ?";

        jdbcTemplate.batchUpdate(bulkUpdateSql, updateDtoList, updateDtoList.size(),
                (PreparedStatement ps, PostViewUpdateDto dto) -> {
                    ps.setInt(1, dto.viewCountDelta());
                    ps.setLong(2, dto.postId());
                });
    }

}
