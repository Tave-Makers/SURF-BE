package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.member.exception.MisMatchPasswordException;
import com.tavemakers.surf.domain.member.exception.PasswordEncryptionException;
import com.tavemakers.surf.global.common.encoder.HexEncoder;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Embeddable
public class Password {

    @Column(name = "password")
    private final String value;

    protected Password() {
        this.value = null;
    }

    public static Password from(String password) {
        return new Password(encrypt(password));
    }

    private static String encrypt(String password) {
        try {
            final String algorithm = "SHA-256";
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] bytes = password.getBytes();
            byte[] digest = md.digest(bytes);
            return HexEncoder.convertHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new PasswordEncryptionException();
        }
    }

    public void validateMatches(String password) {
        if (isNotMatches(password)) {
            throw new MisMatchPasswordException();
        }
    }

    private boolean isNotMatches(String password) {
        return !encrypt(password).equals(this.value);
    }
}