package com.tavemakers.surf.domain.auth.apple.service;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Apple JWKS 공개키 소스 — Nimbus {@link RemoteJWKSet} 기반.
 * <p>내부 캐시 + {@code kid} 미스 시 자동 재조회까지 Nimbus가 처리한다.
 */
@Slf4j
@Service
public class AppleJwksProvider {

    private static final String JWKS_URL = "https://appleid.apple.com/auth/keys";

    private JWKSource<SecurityContext> jwkSource;

    @PostConstruct
    void init() {
        try {
            DefaultResourceRetriever retriever = new DefaultResourceRetriever(2_000, 5_000);
            this.jwkSource = new RemoteJWKSet<>(new URL(JWKS_URL), retriever);
            log.info("[APPLE][JWKS] initialized url={}", JWKS_URL);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Apple JWKS URL is malformed", e);
        }
    }

    /** identityToken 검증에 사용할 JWKS 소스를 반환한다. */
    public JWKSource<SecurityContext> getJwkSource() {
        return jwkSource;
    }
}
