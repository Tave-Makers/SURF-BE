package com.tavemakers.surf.domain.board.repository;

import com.tavemakers.surf.domain.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {
}
