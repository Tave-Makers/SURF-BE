package com.tavemakers.surf.infrastructure.post.repository;

import com.tavemakers.surf.domain.post.dto.PostViewUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/** viewCount 델타 벌크 반영용 JDBC 구체 레포지토리 (포트 추상화 없음 — JPA 교체 계획 없음) */
@Repository
@RequiredArgsConstructor
public class PostJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

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
