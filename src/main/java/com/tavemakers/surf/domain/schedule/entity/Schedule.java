package com.tavemakers.surf.domain.schedule.entity;

import com.tavemakers.surf.presentation.schedule.dto.request.ScheduleCreateReqDTO;
import com.tavemakers.surf.presentation.schedule.dto.request.ScheduleUpdateReqDTO;
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

    public static Schedule from(ScheduleCreateReqDTO dto) {
        validateScheduleTime(dto.startAt(), dto.endAt());
        return of(dto, null);
    }

    public static Schedule of(ScheduleCreateReqDTO dto, Post post) {
        validateScheduleTime(dto.startAt(), dto.endAt());
        return Schedule.builder()
                .category(dto.category())
                .title(dto.title())
                .startAt(dto.startAt())
                .endAt(dto.endAt())
                .location(dto.location())
                .post(post)
                .build();
    }

    public void updateSchedule(ScheduleUpdateReqDTO dto){
        if(dto.startAt()!=null && dto.endAt()!=null){
            validateScheduleTime(dto.startAt(), dto.endAt());
            this.startAt = dto.startAt();
            this.endAt = dto.endAt();
        }
        else if(dto.startAt() != null) {
            validateScheduleTime(dto.startAt(), this.endAt);
            this.startAt = dto.startAt();
        } else if (dto.endAt() != null) {
            validateScheduleTime(this.startAt, dto.endAt());
            this.endAt = dto.endAt();
        }

        if(dto.category() != null) {
            this.category = dto.category();
        }

        if(dto.title() != null) {
            this.title = dto.title();
        }

        if(dto.location() != null) {
            this.location = dto.location();
        }
    }

    private static void validateScheduleTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt.isAfter(endAt)) {
            throw new ScheduleTimeException();
        }
    }
}
