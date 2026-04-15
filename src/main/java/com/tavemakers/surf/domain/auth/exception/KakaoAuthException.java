package com.tavemakers.surf.domain.auth.exception;

import org.springframework.http.HttpStatus;

public class KakaoAuthException extends RuntimeException {
  public KakaoAuthException(HttpStatus status, String message) {
    super(message);
  }
}
