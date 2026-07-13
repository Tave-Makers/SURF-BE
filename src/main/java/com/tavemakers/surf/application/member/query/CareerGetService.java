package com.tavemakers.surf.application.member.query;

import com.tavemakers.surf.presentation.member.dto.response.CareerResDTO;
import com.tavemakers.surf.domain.member.entity.Career;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerGetService {

    private final CareerRepository careerRepository;

    /** 회원 ID로 경력 엔티티 목록 조회 */
    public List<Career> getMyCareers(Long memberId) {
        return careerRepository.findByMemberId(memberId);
    }

    /** 회원 ID로 경력 DTO 목록 조회 */
    public List<CareerResDTO> getMemberCareers(Long memberId) {
        return careerRepository.findByMemberId(memberId)
                .stream()
                .map(CareerResDTO::from)
                .toList();
    }

}
