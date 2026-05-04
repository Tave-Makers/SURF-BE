package com.tavemakers.surf.domain.auth.apple.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.tavemakers.surf.domain.auth.apple.config.AppleOAuthProps;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthErrorMessage;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthException;
import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.HexFormat;

/**
 * Apple identityToken 검증 서비스 (D3, D4).
 * <p>RS256 서명 + iss/aud/exp/nonce/sub 5단 검증. 흐름별 audience 분기 (D4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleIdentityTokenVerifier {

    private static final String EXPECTED_ISSUER = "https://appleid.apple.com";

    private final AppleJwksProvider jwksProvider;
    private final AppleOAuthProps props;

    /**
     * Apple identityToken 검증 후 사용자 정보 추출.
     * @param idToken        Apple 발급 RS256 JWT
     * @param clientType     APP=Bundle ID, WEB=Service ID aud 검증 (D4)
     * @param rawNonce       클라이언트가 전달한 원본 nonce. APP 흐름은 SHA-256(hex) 후 claim과 비교, WEB 흐름은 평문 비교.
     */
    public OAuthUserInfoDTO verifyAndExtract(String idToken, ClientType clientType, String rawNonce) {
        try {
            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwksProvider.getJwkSource());
            processor.setJWSKeySelector(keySelector);

            JWTClaimsSet claims = processor.process(idToken, null);
            validateClaims(claims, clientType, rawNonce);

            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new AppleAuthException(
                        AppleAuthErrorMessage.SUBJECT_MISSING.getStatus(),
                        AppleAuthErrorMessage.SUBJECT_MISSING.getMessage()
                );
            }
            String email = (String) claims.getClaim("email");
            log.info("[APPLE][VERIFY] token verified sub={}", sub);
            return new OAuthUserInfoDTO(sub, email, null, null);

        } catch (AppleAuthException e) {
            throw e;
        } catch (BadJOSEException | JOSEException | ParseException e) {
            log.warn("[APPLE][VERIFY] token verification failed: {}", e.getMessage());
            throw new AppleAuthException(
                    AppleAuthErrorMessage.IDENTITY_TOKEN_INVALID.getStatus(),
                    AppleAuthErrorMessage.IDENTITY_TOKEN_INVALID.getMessage()
            );
        }
    }

    private void validateClaims(JWTClaimsSet claims, ClientType clientType, String rawNonce) {
        if (!EXPECTED_ISSUER.equals(claims.getIssuer())) {
            throw new AppleAuthException(
                    AppleAuthErrorMessage.INVALID_ISSUER.getStatus(),
                    AppleAuthErrorMessage.INVALID_ISSUER.getMessage()
            );
        }
        String expectedAud = (clientType == ClientType.APP)
                ? props.getAppBundleId()
                : props.getServiceClientId();
        if (!claims.getAudience().contains(expectedAud)) {
            throw new AppleAuthException(
                    AppleAuthErrorMessage.INVALID_AUDIENCE.getStatus(),
                    AppleAuthErrorMessage.INVALID_AUDIENCE.getMessage()
            );
        }
        if (new Date().after(claims.getExpirationTime())) {
            throw new AppleAuthException(
                    AppleAuthErrorMessage.TOKEN_EXPIRED.getStatus(),
                    AppleAuthErrorMessage.TOKEN_EXPIRED.getMessage()
            );
        }
        // APP: 클라이언트가 SHA-256(rawNonce)를 Apple로 보내므로 서버에서 해싱 후 비교
        // WEB: 서버가 rawNonce를 authorize URL에 직접 삽입 → Apple이 그대로 echo
        String nonceClaim = (String) claims.getClaim("nonce");
        String expectedNonce = (clientType == ClientType.APP)
                ? sha256Hex(rawNonce)
                : rawNonce;
        if (!expectedNonce.equals(nonceClaim)) {
            throw new AppleAuthException(
                    AppleAuthErrorMessage.INVALID_NONCE.getStatus(),
                    AppleAuthErrorMessage.INVALID_NONCE.getMessage()
            );
        }
    }

    private String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
