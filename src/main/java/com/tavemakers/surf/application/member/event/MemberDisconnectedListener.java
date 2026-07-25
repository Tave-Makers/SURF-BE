package com.tavemakers.surf.application.member.event;

import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent;

import com.tavemakers.surf.infrastructure.auth.apple.AppleApiClient;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 회원 연결 해제 후처리 — 커밋 이후(AFTER_COMMIT) 비동기로 refresh 토큰 무효화와
 * 외부 연결 해제(Kakao unlink / Apple revoke)를 수행한다.
 *
 * <p>탈퇴/제명 트랜잭션 안에서 외부 HTTP를 호출하면 응답 지연이 DB 커넥션을 점유하고,
 * 외부 해제 성공 후 트랜잭션이 롤백되면 되돌릴 수 없는 불일치가 생긴다.
 * 커밋 이후로 미뤄 두 문제를 모두 제거한다.
 *
 * <p>이벤트의 소셜 계정 스냅샷을 순회해 다중 연결(Kakao+Apple) 회원도 양쪽 모두 해제하며,
 * 한 provider 실패가 다른 provider 해제·refresh 토큰 정리를 막지 않도록 단계별로 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberDisconnectedListener {

    private final RefreshTokenService refreshTokenService;
    private final AppleApiClient appleApiClient;

    @Qualifier("kakaoRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${kakao.admin-key}")
    private String adminKey;

    @Value("${kakao.unlink-uri}")
    private String unlinkUri;

    /** 커밋 이후 refresh 토큰을 무효화하고 연결된 각 provider(Kakao/Apple)를 해제한다. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemberDisconnectedEvent event) {
        invalidateRefreshTokens(event.memberId());

        for (MemberDisconnectedEvent.SocialAccountSnapshot account : event.socialAccounts()) {
            disconnectProvider(event.memberId(), account);
        }
    }

    /** refresh 토큰 정리 실패가 provider 연결 해제를 막지 않도록 격리한다. */
    private void invalidateRefreshTokens(Long memberId) {
        try {
            refreshTokenService.invalidateAll(memberId);
        } catch (Exception e) {
            log.error("[MEMBER][DISCONNECT] refresh 토큰 무효화 실패 memberId={}", memberId, e);
        }
    }

    /** 한 provider 실패가 다른 provider 해제를 막지 않도록 격리한다. */
    private void disconnectProvider(Long memberId, MemberDisconnectedEvent.SocialAccountSnapshot account) {
        try {
            if (account.provider() == Provider.KAKAO) {
                unlinkKakao(account.kakaoId());
            } else if (account.provider() == Provider.APPLE) {
                revokeApple(account.appleRefreshToken());
            }
        } catch (Exception e) {
            log.error("[MEMBER][DISCONNECT] provider 연결 해제 실패 memberId={}, provider={}",
                    memberId, account.provider(), e);
        }
    }

    private void revokeApple(String appleRefreshToken) {
        if (appleRefreshToken == null) {
            log.warn("[APPLE][REVOKE] appleRefreshToken 없음 — revoke 생략");
            return;
        }
        // App(Bundle ID)·Web(Service ID)를 각각 독립 시도 — 한쪽 실패가 다른 쪽 시도를 막지 않는다
        // Apple /auth/revoke는 잘못된 client_id로 호출해도 HTTP 200을 반환하므로 에러 로그 오염 없음
        revokeAppleQuietly(() -> appleApiClient.revokeAppToken(appleRefreshToken), "revokeAppToken");
        revokeAppleQuietly(() -> appleApiClient.revokeToken(appleRefreshToken), "revokeToken");
    }

    /** Apple revoke 단계 격리 — 한 호출 실패가 다음 호출을 막지 않도록 개별 보호한다. */
    private void revokeAppleQuietly(Runnable revoke, String label) {
        try {
            revoke.run();
        } catch (Exception e) {
            log.error("[APPLE][REVOKE] {} 실패", label, e);
        }
    }

    private void unlinkKakao(Long kakaoId) {
        if (kakaoId == null) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", String.valueOf(kakaoId));

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(unlinkUri, entity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KAKAO][UNLINK] 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("[KAKAO][UNLINK] 예기치 못한 오류", e);
        }
    }
}
