package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.dto.CareerCreateCommand;
import com.tavemakers.surf.domain.member.entity.Career;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerCreateService {

    private final CareerRepository careerRepository;

    //경력 신규 생성
    public void createCareer(Member member, List<CareerCreateCommand> commands) {
        List<Career> newCareers = commands.stream()
                .map(command -> Career.of(
                        command.companyName(),
                        command.position(),
                        command.startDate(),
                        command.endDate(),
                        command.isWorking(),
                        member))
                .toList();
        careerRepository.saveAll(newCareers);
    }
}
