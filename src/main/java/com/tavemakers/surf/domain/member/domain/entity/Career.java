package com.tavemakers.surf.domain.member.domain.entity;

import com.tavemakers.surf.domain.member.presentation.dto.request.CareerCreateReqDTO;
import com.tavemakers.surf.domain.member.presentation.dto.request.CareerUpdateReqDTO;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Career extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "career_id") // ERD의 '경력_id'에 맞춰 컬럼명 지정
    private Long id;

    @Column(nullable = false)
    private String companyName; // 회사명

    @Column(nullable = false)
    private String position; // 직무

    @Column(nullable = false)
    private LocalDate startDate; // 근무 시작일

    private LocalDate endDate; // 근무 종료일 (진행 중일 경우 null)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    private boolean isWorking;

    //경력 수정
    public void update(CareerUpdateReqDTO dto) {
        if (dto.companyName() != null) {
            this.companyName = dto.companyName();
        }
        if (dto.position() != null) {
            this.position = dto.position();
        }
        if (dto.startDate() != null) {
            this.startDate = dto.startDate();
        }
        if (dto.endDate() != null) {
            this.endDate = dto.endDate();
        }
        if (dto.isWorking() != null) {
            this.isWorking = dto.isWorking();
            if (this.isWorking) {
                this.endDate = null;
            }
        }
    }

    //정적 팩토리 메소드 - 생성
    public static Career of(CareerCreateReqDTO dto, Member member) {
        return Career.builder()
                .companyName(dto.companyName())
                .position(dto.position())
                .startDate(dto.startDate())
                .endDate(dto.endDate() != null ? dto.endDate() : null)
                .member(member)
                .isWorking(dto.isWorking())
                .build();
    }

}
