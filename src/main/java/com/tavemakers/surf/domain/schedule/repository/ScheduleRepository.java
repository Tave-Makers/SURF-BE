package com.tavemakers.surf.domain.schedule.repository;

import com.tavemakers.surf.domain.schedule.entity.Schedule;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByStartAtBetweenOrderByStartAtAscIdAsc(LocalDateTime start, LocalDateTime end);

    List<Schedule> findByStartAtBetweenAndCategoryInOrderByStartAtAscIdAsc(
            LocalDateTime start, LocalDateTime end, List<String> categories);

    /** 일정 태그와 삭제가 동시에 실행될 때 삭제된 일정에 연결되는 것을 방지한다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Schedule s where s.id = :id")
    Optional<Schedule> findByIdForUpdate(@Param("id") Long id);

    Optional<Schedule> findFirstByCategoryAndStartAtAfterOrderByStartAtAsc(
            String category, LocalDateTime now);

    Optional<Schedule> findFirstByCategoryAndStartAtLessThanEqualOrderByStartAtDesc(
            String category, LocalDateTime now);
}
