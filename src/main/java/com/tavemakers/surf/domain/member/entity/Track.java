package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.global.common.entity.BaseEntity;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track extends BaseEntity implements Comparable<Track>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_id")
    private Long id;

    @Column(nullable = false)
    private Integer generation; // 기수

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Part part; // 파트 (e.g., BACKEND, FRONTEND)

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계 설정
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public Track(Integer generation, Part part) {
        this.generation = generation;
        this.part = part;
    }

    public void setMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("member는 null일 수 없습니다.");
        }

        if (this.member == member) return;

        // 기존 관계 끊기
        if (this.member != null) {
            this.member.getTracks().remove(this);
        }

        this.member = member;

        // 양방향 일관성 유지
        if (!member.getTracks().contains(this)) {
        member.getTracks().add(this);
        }
    }

    public void update(Integer generation, Part part) {
        if (generation != null) this.generation = generation;
        if (part != null) this.part = part;
    }

    @Override
    public int compareTo(Track o) {
        return this.generation.compareTo(o.getGeneration());
    }

}
