package com.tavemakers.surf.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/login/")
                || uri.equals("/auth/logout")
                || uri.equals("/auth/refresh")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String accessToken = jwtService.extractAccessTokenFromHeader(request).orElse(null);

        // 1) AT가 아예 없으면 → 익명 통과 (인가 여부는 SecurityConfig가 판단)
        if (accessToken == null) {
            chain.doFilter(request, response);
            return;
        }

        // 2) AT가 있는데 유효하지 않으면 → 401 (프론트가 /auth/refresh 후 재시도)
        if (!jwtService.isTokenValid(accessToken)) {
            unauthorized(response, "Invalid or expired access token");
            return;
        }

        // 3) member 로드 + 탈퇴 판별 + 인증 주입
        AuthResult result = authenticateUser(accessToken, request);

        if (result != AuthResult.AUTHENTICATED) {
            unauthorized(response, result.message);
            return;
        }
        chain.doFilter(request, response);
    }

    private enum AuthResult {
        AUTHENTICATED(""),
        NOT_FOUND("Member not found"),
        WITHDRAWN("Withdrawn member");

        final String message;
        AuthResult(String message) { this.message = message; }
    }

    private AuthResult authenticateUser(String accessToken, HttpServletRequest req) {
        Long memberId = jwtService.extractMemberId(accessToken).orElse(null);
        if (memberId == null) return AuthResult.NOT_FOUND;

        return memberRepository.findById(memberId)
                .map(member -> {
                    if (member.getStatus() == MemberStatus.WITHDRAWN) {
                        return AuthResult.WITHDRAWN;
                    }

                    CustomUserDetails principal = new CustomUserDetails(member);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null, principal.getAuthorities()
                            );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);

                    return AuthResult.AUTHENTICATED;
                })
                .orElse(AuthResult.NOT_FOUND);
    }

    private void unauthorized(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(Map.of("message", message)));
    }
}