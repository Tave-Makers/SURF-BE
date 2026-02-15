package com.tavemakers.surf.domain.group.repository;

import com.tavemakers.surf.domain.group.entity.Group;
import com.tavemakers.surf.domain.group.entity.GroupType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    // 목록 조회: leader만 필요
    @EntityGraph(attributePaths = {"leader"})
    @Query("""
        select g
          from Group g
         where g.generation = :generation
           and (:type is null or g.type = :type)
         order by g.id desc
    """)
    List<Group> findList(@Param("generation") Integer generation, @Param("type") GroupType type);

    // 상세 조회: 멤버십 + 멤버까지 필요
    @EntityGraph(attributePaths = {"leader", "groupMembers", "groupMembers.member"})
    Optional<Group> findDetailById(Long id);
}