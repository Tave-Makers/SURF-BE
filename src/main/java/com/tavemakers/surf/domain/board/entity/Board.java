package com.tavemakers.surf.domain.board.entity;

import com.tavemakers.surf.domain.board.dto.request.BoardCreateReqDTO;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private BoardType type;

    @Builder
    private Board(String name, BoardType type) {
        this.name = name;
        this.type = type;
    }

    public static Board of(BoardCreateReqDTO req) {
        return Board.builder()
                .name(req.name())
                .type(req.type())
                .build();
    }

    public void update(String name, BoardType type) {
        this.name = name;
        this.type = type;
    }

    public boolean isNotice() {
        return type == BoardType.NOTICE;
    }

    public boolean isGeneral() {
        return type == BoardType.GENERAL;
    }
}
