package com.tavemakers.surf.domain.team.application.query;

import com.tavemakers.surf.domain.team.domain.entity.Team;
import com.tavemakers.surf.domain.team.domain.exception.TeamNotFoundException;
import com.tavemakers.surf.domain.team.domain.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamGetService {

    private final TeamRepository teamRepository;

    /** 기수별 팀 목록 조회 (멤버 포함) */
    public List<Team> getTeamsWithMembers(Integer generation) {
        return teamRepository.findTeamsWithMembers(generation);
    }

    /** 팀 단건 조회 (멤버 포함) */
    public Team getTeamWithMembers(Long teamId) {
        return teamRepository.findDetailBaseById(teamId)
                .orElseThrow(TeamNotFoundException::new);
    }

}
