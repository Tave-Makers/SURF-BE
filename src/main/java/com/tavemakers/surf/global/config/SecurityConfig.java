package com.tavemakers.surf.global.config;

import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.global.jwt.JwtAuthenticationFilter;
import com.tavemakers.surf.global.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 전역 설정
 */
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PermitUrlConfig permitUrlConfig;


    // 인증 없이 접근 가능한 URL 정의
    private static final String[] PERMITTED_URLS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",

            "/kakao/login",
            "/login/oauth2/code/kakao",
            "/login/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(
                corsConfigurationSource()));

        http
                .csrf(csrf -> csrf.disable()) // JWT 사용 시 CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login/**").permitAll() // 로그인
                        .requestMatchers("/auth/refresh").permitAll()   //
                        .requestMatchers("/auth/logout").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        .requestMatchers(permitUrlConfig.getPublicUrl()).permitAll()
                        .requestMatchers(permitUrlConfig.getMemberUrl()).hasAnyRole("MEMBER", "ADMIN", "PRESIDENT", "MANAGER")
                        .requestMatchers(permitUrlConfig.getAdminUrl()).hasAnyRole("ADMIN", "PRESIDENT", "MANAGER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable()) // 우리는 소셜 로그인 + JWT 사용 → formLogin 비활성화
                .httpBasic(basic -> basic.disable()) // Basic Auth 비활성화
                .headers(h -> h.frameOptions(f -> f.disable())); // H2 console 접근 허용 필요 시

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apple form_post 콜백 전용: appleid.apple.com이 브라우저를 통해 cross-site POST를 보냄
        // form_post는 XHR이 아니므로 allowCredentials 불필요, POST만 허용
        CorsConfiguration appleCallback = new CorsConfiguration();
        appleCallback.setAllowedOrigins(Arrays.asList("https://appleid.apple.com"));
        appleCallback.setAllowedMethods(Arrays.asList("POST"));
        appleCallback.setAllowedHeaders(Arrays.asList("*"));
        appleCallback.setAllowCredentials(false);
        source.registerCorsConfiguration("/login/oauth2/code/apple", appleCallback);

        // 일반 API 설정
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://139.150.81.126.nip.io",
                "https://139.150.81.126.nip.io",
                "https://tavesurf.site"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "RefreshToken", "Content-Type", "Set-Cookie"));
        configuration.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, memberRepository);
    }

}