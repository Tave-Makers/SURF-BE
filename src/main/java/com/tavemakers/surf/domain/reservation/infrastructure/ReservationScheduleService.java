package com.tavemakers.surf.domain.reservation.infrastructure;

import com.tavemakers.surf.domain.reservation.application.task.PostPublishRunner;
import com.tavemakers.surf.domain.reservation.application.task.PostPublishTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationScheduleService {

    private final PostPublishRunner taskRunner;
    private final TaskScheduler taskScheduler;

    /** 예약 게시글 발행 스케줄 등록 */
    public void schedule(Long reservationId, Instant publishAt) {
        PostPublishTask task = PostPublishTask.of(reservationId, taskRunner);
        taskScheduler.schedule(task, publishAt);
    }

}
