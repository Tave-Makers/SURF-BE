package com.tavemakers.surf.global.common.loader;

import com.tavemakers.surf.domain.reservation.domain.entity.Reservation;
import com.tavemakers.surf.domain.reservation.application.query.ReservationGetService;
import com.tavemakers.surf.domain.reservation.infrastructure.ReservationScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationStartupLoader implements ApplicationListener<ApplicationReadyEvent> {

    private final ReservationScheduleService scheduleService;
    private final ReservationGetService reservationGetService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<Reservation> reservedInformation = reservationGetService.getAllReservation();
        if (reservedInformation.isEmpty()) {
            log.info("복구할 예약 작업이 없습니다.");
            return;
        }

        for(Reservation reservation : reservedInformation) {
            scheduleService.schedule(reservation.getId(), reservation.getReservedAt());
        }
    }

}
