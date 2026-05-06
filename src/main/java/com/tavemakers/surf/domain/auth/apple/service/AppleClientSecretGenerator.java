package com.tavemakers.surf.domain.auth.apple.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tavemakers.surf.domain.auth.apple.config.AppleOAuthProps;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthErrorMessage;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Apple client_secret JWT 생성 (ES256, D7).
 * <p>P8 PrivateKey를 Base64 환경변수에서 메모리 1회 로딩. 매 요청 시 새 JWT 생성(5분 만료).
 */
@Slf4j
@Service
public class AppleClientSecretGenerator {

    private final AppleOAuthProps props;
    private ECPrivateKey privateKey;

    public AppleClientSecretGenerator(AppleOAuthProps props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        try {
            byte[] pemBytes = Base64.getDecoder().decode(props.getPrivateKey());
            String pem = new String(pemBytes, StandardCharsets.UTF_8);
            String pkcs8 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(pkcs8);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            this.privateKey = (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(spec);
            log.info("[APPLE][CLIENT-SECRET] private key loaded successfully");
        } catch (Exception e) {
            log.warn("[APPLE][CLIENT-SECRET] private key loading failed — Apple Web login unavailable: {}",
                    e.getMessage());
            this.privateKey = null;
        }
    }

    /** Apple /auth/token 호출용 client_secret JWT를 생성한다 (ES256, exp 5분). */
    public String generate() {
        if (privateKey == null) {
            throw new AppleAuthException(
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getStatus(),
                    "Apple client secret key not configured"
            );
        }
        try {
            long now = System.currentTimeMillis() / 1000;
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(props.getKeyId())
                    .build();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(props.getTeamId())
                    .issueTime(new Date(now * 1000))
                    .expirationTime(new Date((now + 300) * 1000))
                    .audience(List.of("https://appleid.apple.com"))
                    .subject(props.getServiceClientId())
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            log.error("[APPLE][CLIENT-SECRET] generation failed", e);
            throw new AppleAuthException(
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getStatus(),
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getMessage()
            );
        }
    }
}
