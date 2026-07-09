package com.tavemakers.surf.domain.reservation.entity;

import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private Instant reservedAt;

    public static Reservation of(Long postId, Instant reservedAt) {
        return Reservation.builder()
                .postId(postId)
                .reservedAt(reservedAt)
                .status(ReservationStatus.RESERVED)
                .build();
    }

    public void publish() {
        status = ReservationStatus.PUBLISHED;
    }

    public void cancel() {
        status = ReservationStatus.CANCELLED;
    }

}
