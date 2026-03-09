package com.project.blinddate.chat.config;

import com.project.blinddate.chat.filter.RequestLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configure(http))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // JWT를 사용하므로 서버에서 세션을 생성하지 않도록 SessionCreationPolicy.STATELESS 를 설정
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers
                // 클릭재킹 방지를 위해 동일 도메인 내에서만 iframe 사용을 허용( SAMEORIGIN )
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                // 브라우저의 XSS 필터를 강제로 활성화하고 공격 감지 시 차단하도록 설정
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                // 신뢰할 수 있는 소스(jQuery, FontAwesome 등)에서만 스크립트와 스타일을 불러올 수 있도록 엄격한 보안 정책을 적용
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://code.jquery.com https://cdnjs.cloudflare.com; " +
                        "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                        "font-src 'self' https://cdnjs.cloudflare.com; " +
                        "img-src 'self' data: https://randomuser.me https://minio.blind-date.com https://minio.blind-date; " +
                        "connect-src 'self' http://*.blind-date.com https://*.blind-date.com https://cdnjs.cloudflare.com;"
                ))
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
                    .requestMatchers(new OrRequestMatcher(
                            new AntPathRequestMatcher("/actuator/**"),
                            new AntPathRequestMatcher("/swagger-ui/**")
                    ))
                    .access((authentication, context) -> {
                        String remoteAddr = context.getRequest().getRemoteAddr();
                        // 허용할 IP/대역 목록 (로컬 및 사설 네트워크 대역 허용)
                        List<IpAddressMatcher> allowedIps = List.of(
                                new IpAddressMatcher("127.0.0.1"),    // 로컬 접속
                                new IpAddressMatcher("10.0.0.0/8"),    // 사설 IP 대역 A
                                new IpAddressMatcher("172.16.0.0/12"), // 사설 IP 대역 B (Docker 기본 대역)
                                new IpAddressMatcher("192.168.0.0/16") // 사설 IP 대역 C
                        );

                        boolean allowed = allowedIps.stream().anyMatch(m -> m.matches(remoteAddr));
                        return new AuthorizationDecision(allowed);
                    })
                    .anyRequest().permitAll()                                // 다른 요청은 모두 허용
            );

        http.addFilterBefore(new RequestLoggingFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
