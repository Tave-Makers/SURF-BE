package com.tavemakers.surf.domain.feedback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class WriterHashService {
    @Value("${feedback.hash.secret}")
    private String secret;

    /** 회원 ID와 날짜 기반 익명 해시 생성 */
    public String hashDaily(Long memberId, LocalDate date) {
        return hmacSha256Hex(secret, "M|" + memberId + "|" + date);
    }

    private String hmacSha256Hex(String key, String message) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}