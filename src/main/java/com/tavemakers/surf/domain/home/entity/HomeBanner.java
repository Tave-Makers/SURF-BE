package com.tavemakers.surf.domain.home.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false, length = 1000)
    private String linkUrl;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean status = true; // 활성화 여부

    public static HomeBanner of(String name, String imageUrl, String linkUrl, Integer displayOrder) {
        return HomeBanner.builder()
                .name(name)
                .imageUrl(imageUrl)
                .linkUrl(linkUrl)
                .displayOrder(displayOrder)
                .status(true)
                .build();
    }

    public void updateBanner(String name, String imageUrl, String linkUrl) {
        validateUpdate(name, imageUrl, linkUrl);

        this.name = name;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
    }

    public void changeDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void activate() {
        this.status = true;
    }

    public void deactivate() {
        this.status = false;
    }

    private void validateUpdate(String name, String imageUrl, String linkUrl) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 null이거나 빈 값일 수 없습니다.");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl은 null이거나 빈 값일 수 없습니다.");
        }
        if (linkUrl == null || linkUrl.isBlank()) {
            throw new IllegalArgumentException("linkUrl은 null이거나 빈 값일 수 없습니다.");
        }
    }
}