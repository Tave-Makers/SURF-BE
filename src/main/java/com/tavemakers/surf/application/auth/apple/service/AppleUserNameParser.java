package com.tavemakers.surf.application.auth.apple.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Apple Web 콜백의 {@code user} 폼 필드에서 사용자 이름을 추출한다.
 *
 * <p>Apple은 이름을 id_token 클레임이 아니라 {@code user} JSON
 * ({@code {"name":{"firstName":"길동","lastName":"홍"},"email":...}})으로만,
 * 그것도 <b>해당 Apple ID의 최초 인가 시 딱 한 번만</b> 전달한다 (이슈 #392).
 *
 * <p>파싱 실패가 로그인 전체를 실패시키면 안 되므로 어떤 입력이든 예외 없이 null을 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleUserNameParser {

    private final ObjectMapper objectMapper;

    /** user JSON에서 이름 추출 — 한국어 표기 순서(lastName + firstName)로 조합, 실패 시 null */
    public String extractName(String userPayload) {
        if (userPayload == null || userPayload.isBlank()) {
            return null;
        }

        try {
            JsonNode name = objectMapper.readTree(userPayload).path("name");
            String lastName = name.path("lastName").asText("").trim();
            String firstName = name.path("firstName").asText("").trim();

            String fullName = (lastName + firstName).trim();
            return fullName.isBlank() ? null : fullName;
        } catch (Exception e) {
            // 이름은 부가 정보 — 파싱 실패로 로그인을 막지 않는다
            log.warn("[LOGIN][APPLE] user 페이로드 파싱 실패 — 이름 없이 진행", e);
            return null;
        }
    }
}
