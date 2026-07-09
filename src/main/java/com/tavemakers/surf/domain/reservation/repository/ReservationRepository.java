package com.tavemakers.surf.domain.reservation.repository;

import com.tavemakers.surf.domain.reservation.entity.Reservation;
import com.tavemakers.surf.domain.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByIdAndStatus(Long id, ReservationStatus status);

    List<Reservation> findByStatus(ReservationStatus status);

    Optional<Reservation> findByPostIdAndStatus(Long postId, ReservationStatus status);

    /** 예약 변경용 행 잠금 조회 — 잠금 읽기로 직전 커밋된 최신 상태(선행 요청의 cancel/신규 예약)를 읽는다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.postId = :postId and r.status = :status")
    Optional<Reservation> findByPostIdAndStatusForUpdate(
            @Param("postId") Long postId, @Param("status") ReservationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Reservation r WHERE r.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);

}
