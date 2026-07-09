package com.tavemakers.surf.domain.activity.entity;

import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActiveGeneration extends BaseEntity {

    public static final Long ID = 1L; // id 값은 항상 1로 고정 (활동 기수는 하나만 존재)

    @Id
    private Long id;

    @Column(nullable = false)
    private Integer generation;


    public static ActiveGeneration init(Integer generation) {
        ActiveGeneration ag = new ActiveGeneration();
        ag.id = ID;
        ag.generation = generation;
        return ag;
    }

    public void updateGeneration(Integer generation) {
        this.generation = generation;
    }
}