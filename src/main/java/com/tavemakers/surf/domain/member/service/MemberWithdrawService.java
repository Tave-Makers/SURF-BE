package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberWithdrawService {

    private final MemberGetService memberGetService;
    private final RefreshTokenService refreshTokenService;

    private final RestTemplate restTemplate;

    @Value("${kakao.admin-key}")
    private String adminKey;

    @Value("${kakao.unlink-uri}")
    private String unlinkUri;

    /** 회원 탈퇴 처리 및 카카오 연결 해제 */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberGetService.getMember(memberId);

        refreshTokenService.invalidateAll(memberId);

        unlinkKakao(member.getKakaoId());

        if (member.getStatus() != MemberStatus.WITHDRAWN) {
            member.withdraw();
        }
    }

    private void unlinkKakao(Long kakaoId) {
        if (kakaoId == null) {
            return;
        }

        String url = unlinkUri;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", String.valueOf(kakaoId));

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KAKAO][UNLINK] 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("[KAKAO][UNLINK] 예기치 못한 오류", e);
        }
    }
}