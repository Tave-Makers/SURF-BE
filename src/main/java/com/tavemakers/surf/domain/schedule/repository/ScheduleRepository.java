package com.tavemakers.surf.domain.schedule.repository;

import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByStartAtBetween(LocalDateTime startOfMonth, LocalDateTime endOfMonth);

    List<Schedule> findByStartAtBetweenAndCategoryIn(
            LocalDateTime startOfMonth,
            LocalDateTime endOfMonth,
            List<String> categories
    );

    Optional<Schedule> findByPostId(Long postId);

    void deleteByPost(Post post);

    Optional<Schedule> findFirstByCategoryAndStartAtAfterOrderByStartAtAsc(
            String category,
            LocalDateTime now
    );

    Optional<Schedule> findFirstByCategoryAndStartAtLessThanEqualOrderByStartAtDesc(
            String category,
            LocalDateTime now
    );

    // 관리자용: 모든 카테고리, 예약글 제외
    @Query("SELECT s FROM Schedule s LEFT JOIN s.post p " +
           "WHERE s.startAt BETWEEN :startOfMonth AND :endOfMonth " +
           "AND (p IS NULL OR p.isReserved = false)")
    List<Schedule> findByStartAtBetweenExcludingReserved(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );

    // 일반 회원용: 특정 카테고리만, 예약글 제외
    @Query("SELECT s FROM Schedule s LEFT JOIN s.post p " +
           "WHERE s.startAt BETWEEN :startOfMonth AND :endOfMonth " +
           "AND s.category IN :categories " +
           "AND (p IS NULL OR p.isReserved = false)")
    List<Schedule> findByStartAtBetweenAndCategoryInExcludingReserved(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth,
            @Param("categories") List<String> categories
    );
}
