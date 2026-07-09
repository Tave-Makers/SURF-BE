package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.presentation.member.dto.request.CareerCreateReqDTO;
import com.tavemakers.surf.domain.member.entity.Career;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerCreateService {

    private final CareerRepository careerRepository;

    //경력 신규 생성
    @Transactional
    public void createCareer(Member member, List<CareerCreateReqDTO> dtos) {
        List<Career> newCareers = dtos.stream()
                .map(dto -> Career.of(dto, member))
                .toList();
        careerRepository.saveAll(newCareers);
    }
}
