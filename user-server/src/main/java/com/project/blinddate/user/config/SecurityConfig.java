package com.project.blinddate.user.config;

import com.project.blinddate.user.filter.RequestLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
