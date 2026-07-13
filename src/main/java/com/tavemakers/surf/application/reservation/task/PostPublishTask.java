package com.tavemakers.surf.application.reservation.task;

public class PostPublishTask implements Runnable {

    private final Long reservationId;
    private final PostPublishRunner publishRunner;

    private PostPublishTask(final Long reservationId, final PostPublishRunner publishRunner) {
        this.reservationId = reservationId;
        this.publishRunner = publishRunner;
    }

    public static PostPublishTask of(final Long reservationId, final PostPublishRunner publishRunner) {
        return new PostPublishTask(reservationId, publishRunner);
    }

    @Override
    public void run() {
        publishRunner.publishPost(reservationId);
    }

}
