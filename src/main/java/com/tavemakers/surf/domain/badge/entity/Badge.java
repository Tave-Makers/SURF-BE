package com.tavemakers.surf.domain.badge.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "badge_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String requirement;

    public Badge(String name, String imageUrl, String description, String requirement) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.description = description;
        this.requirement = requirement;
    }

    public void update(String name, String imageUrl, String description, String requirement) {

        if (name != null && !name.isBlank()) {
            this.name = name;
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            this.imageUrl = imageUrl;
        }

        if (description != null && !description.isBlank()) {
            this.description = description;
        }

        if (requirement != null && !requirement.isBlank()) {
            this.requirement = requirement;
        }
    }
}