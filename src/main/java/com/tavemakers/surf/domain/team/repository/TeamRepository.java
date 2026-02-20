package com.tavemakers.surf.domain.team.repository;

import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("""
        select t
          from Team t
         where (:type is null or t.type = :type)
         order by t.generation desc, t.id desc
    """)
    List<Team> findAllForAccordion(TeamType type);

    @EntityGraph(attributePaths = {
            "leader",
            "teamMembers",
            "teamMembers.member"
    })
    @Query("select t from Team t where t.id = :id")
    Optional<Team> findDetailBaseById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"teamMembers", "teamMembers.member"})
    @Query("select t from Team t where (:generation is null or t.generation = :generation)")
    List<Team> findTeamsWithMembers(@Param("generation") Integer generation);
}