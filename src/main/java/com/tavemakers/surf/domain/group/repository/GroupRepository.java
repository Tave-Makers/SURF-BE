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

    @Query("""
        select g
          from Group g
         where (:type is null or g.type = :type)
         order by g.generation desc, g.id desc
    """)
    List<Group> findAllForAccordion(GroupType type);

    @EntityGraph(attributePaths = {
            "leader",
            "groupMembers",
            "groupMembers.member"
    })
    @Query("select g from Group g where g.id = :id")
    Optional<Group> findDetailBaseById(@Param("id") Long id);
}