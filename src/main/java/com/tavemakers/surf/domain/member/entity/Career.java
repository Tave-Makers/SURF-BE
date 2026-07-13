package com.tavemakers.surf.domain.member.entity;

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
    public void update(String companyName, String position, LocalDate startDate,
                       LocalDate endDate, Boolean isWorking) {
        if (companyName != null) {
            this.companyName = companyName;
        }
        if (position != null) {
            this.position = position;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (isWorking != null) {
            this.isWorking = isWorking;
            if (this.isWorking) {
                this.endDate = null;
            }
        }
    }

    //정적 팩토리 메소드 - 생성
    public static Career of(String companyName, String position, LocalDate startDate,
                            LocalDate endDate, Boolean isWorking, Member member) {
        return Career.builder()
                .companyName(companyName)
                .position(position)
                .startDate(startDate)
                .endDate(endDate)
                .member(member)
                .isWorking(isWorking)
                .build();
    }

}
