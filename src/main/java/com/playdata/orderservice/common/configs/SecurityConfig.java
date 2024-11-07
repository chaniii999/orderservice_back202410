package com.playdata.orderservice.common.configs;

import com.playdata.orderservice.common.auth.JwtAuthFilter;
import com.playdata.orderservice.common.dto.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 권한 검사를 컨트롤러의 메서드에서 전역적으로 수행하기 위한 설정.
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    // 시큐리티 기본 설정 (권한 처리, 초기 로그인 화면 없애기 등등...)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 스프링 시큐리티에서 기본으로 제공하는 CSRF 토큰 공격을 방지하기 위한 장치 해제.
        // CSRF(Cross Site Request Forgery) 사이트 간 요청 위조
        http.csrf(csrfConfig -> csrfConfig.disable());

        http.cors(Customizer.withDefaults()); // 직접 커스텀한 CORS 설정을 적용하겠다.

        // 세션 관리 상태를 사용하지 않고
        // STATELESS한 토큰을 사용하겠다.
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> {
            auth
//                    .requestMatchers("/user/list").hasAnyRole("ADMIN")
                    .requestMatchers("/user/create", "/user/doLogin",
                            "/user/refresh", "/product/list").permitAll()
                    .anyRequest().authenticated();
        })
                // 커스텀 필터를 등록.
                // 시큐리티에서 기본으로 인증, 인가 처리를 해 주는 UsernamePasswordAuthenticationFilter 전에 내 필터 add
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http
                .exceptionHandling(exception -> {
                    // 인증 과정에서 예외가 발생한 경우 그 예외를 핸들링 할 객체를 등록.
                    exception.authenticationEntryPoint(customAuthenticationEntryPoint);
                });


        return http.build();
    }



}
