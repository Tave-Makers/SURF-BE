package com.tavemakers.surf.domain.schedule.entity;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.exception.ScheduleTimeException;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private String location;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public static Schedule from(String category, String title, LocalDateTime startAt, LocalDateTime endAt, String location) {
        return of(category, title, startAt, endAt, location, null);
    }

    public static Schedule of(String category, String title, LocalDateTime startAt, LocalDateTime endAt, String location, Post post) {
        validateScheduleTime(startAt, endAt);
        return Schedule.builder()
                .category(category)
                .title(title)
                .startAt(startAt)
                .endAt(endAt)
                .location(location)
                .post(post)
                .build();
    }

    public void updateSchedule(String category, String title, LocalDateTime startAt, LocalDateTime endAt, String location){
        if(startAt != null && endAt != null){
            validateScheduleTime(startAt, endAt);
            this.startAt = startAt;
            this.endAt = endAt;
        }
        else if(startAt != null) {
            validateScheduleTime(startAt, this.endAt);
            this.startAt = startAt;
        } else if (endAt != null) {
            validateScheduleTime(this.startAt, endAt);
            this.endAt = endAt;
        }

        if(category != null) {
            this.category = category;
        }

        if(title != null) {
            this.title = title;
        }

        if(location != null) {
            this.location = location;
        }
    }

    private static void validateScheduleTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt.isAfter(endAt)) {
            throw new ScheduleTimeException();
        }
    }
}
