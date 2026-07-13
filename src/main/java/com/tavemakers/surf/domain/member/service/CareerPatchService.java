package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.dto.CareerUpdateCommand;
import com.tavemakers.surf.domain.member.entity.Career;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import com.tavemakers.surf.domain.member.validator.CareerValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerPatchService {
    private final CareerRepository careerRepository;
    private final CareerValidator careerValidator;

    /** 회원 경력 정보 수정 */
    public void updateCareer(Member member, List<CareerUpdateCommand> commands) {
        // 요청으로 들어온 ID 목록
        Set<Long> requestedIds = commands.stream()
                .map(CareerUpdateCommand::careerId)
                .collect(Collectors.toSet());

        // 실제 해당 멤버의 Career 중 업데이트 대상 조회
        List<Career> careersToUpdate = careerRepository.findAllByMemberAndIdIn(member, requestedIds);
        careerValidator.validateCareer(requestedIds, careersToUpdate);

        // careerId → Career 매핑
        Map<Long, Career> validCareerMap = careersToUpdate.stream()
                .collect(Collectors.toMap(Career::getId, career -> career));

        // 업데이트 적용
        commands.forEach(command -> {
            Career careerToUpdate = validCareerMap.get(command.careerId());
            if (careerToUpdate != null) {
                careerToUpdate.update(
                        command.companyName(),
                        command.position(),
                        command.startDate(),
                        command.endDate(),
                        command.isWorking());
            }
        });
    }
}