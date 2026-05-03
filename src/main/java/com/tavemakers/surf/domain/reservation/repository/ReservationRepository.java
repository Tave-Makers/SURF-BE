package com.tavemakers.surf.domain.reservation.repository;

import com.tavemakers.surf.domain.reservation.entity.Reservation;
import com.tavemakers.surf.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByIdAndStatus(Long id, ReservationStatus status);

    List<Reservation> findByStatus(ReservationStatus status);

    Optional<Reservation> findByPostIdAndStatus(Long postId, ReservationStatus status);

    void deleteByPostId(Long postId);

}
