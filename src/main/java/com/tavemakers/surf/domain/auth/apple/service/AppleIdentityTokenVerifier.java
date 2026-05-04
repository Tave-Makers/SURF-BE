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

import java.text.ParseException;
import java.util.Date;

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
     * @param rawNonce       클라이언트가 원본 nonce. SHA-256 해시 후 claim과 비교.
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
        // Apple Web 플로우: nonce를 해싱 없이 id_token에 그대로 저장 → rawNonce 직접 비교
        String nonceClaim = (String) claims.getClaim("nonce");
        if (!rawNonce.equals(nonceClaim)) {
            throw new AppleAuthException(
                    AppleAuthErrorMessage.INVALID_NONCE.getStatus(),
                    AppleAuthErrorMessage.INVALID_NONCE.getMessage()
            );
        }
    }
}
