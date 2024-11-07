package com.playdata.orderservice.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
// 클라이언트가 전송한 토큰을 검사하는 필터
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // 필터가 해야 할 일들을 작성.
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 요청과 함께 전달된 토큰을 요청 헤더에서 꺼내기
        String token = parseBearerToken(request);

        log.info("JWT token: {}", token);

        try {
            // 토큰 위조검사 및 인증 완료
            if (token != null) {

                // 토큰 서명 위조 검사와 토큰을 파싱해서 클레임을 얻어내는 작업.
                TokenUserInfo userInfo = jwtTokenProvider.validateAndGetTokenUserInfo(token);

                // spring security에게 전달할 인가 정보 리스트를 생성. (권한 정보)
                // 권한이 여러 개 존재할 경우 리스트로 권한 체크에 사용할 필드를 add. (권한 여러개면 여러번 add 가능)
                // 나중에 컨트롤러의 요청 메서드마다 권한을 파악하게 하기 위해 미리 저장을 해 놓는 것.
                List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
                // ROLE_USER, ROLE_ADMIN (ROLE_ 접두사는 필수입니다.)
                authorityList.add(new SimpleGrantedAuthority("ROLE_" + userInfo.getRole().toString()));


                // 인증 완료 처리
                // spring security에게 인증 정보를 전달해서 전역적으로 어플리케이션 내에서
                // 인증 정보를 활용할 수 있도록 설정.
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userInfo, // 컨트롤러 등에서 활용할 유저 정보
                        "", // 인증된 사용자 비밀번호: 보통 null 혹은 빈 문자열로 선언.
                        authorityList // 인가 정보 (권한)
                );

                // 시큐리티 컨테이너에 인증 정보 객체 등록
                SecurityContextHolder.getContext().setAuthentication(auth);

            }
//            else {
//                // 토큰이 아예 전달되지 않거나, Bearer 형태가 아닐 경우 else로 빠진다.
//                log.error("토큰이 아예 없는데?");
//                response.setStatus(HttpStatus.UNAUTHORIZED.value());
//                response.setContentType("application/json");
//                response.getWriter().write("유효한 토큰 형태가 아니거나 토큰이 없습니다.");
//                return;
//            }

            // 필터를 통과하는 메서드 (doFilter 안하면 필터를 통과하지 못함)
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 토큰 검증 과정에서 문제가 발생한다면 동작할 로직
            log.error(e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("토큰에 문제가 있습니다!");
        }

    }

    private String parseBearerToken(HttpServletRequest request) {

        // 요청 헤더에서 토큰 꺼내오기
        // -- content-type: application/json
        // -- Authorization: Bearer aslkdblk2dnkln34kl52...
        String bearerToken = request.getHeader("Authorization");

        // 아직 순수 토큰값이 아닌 Bearer 이 붙어 있으니 이것을 제거하자.
        // StringUtils.hasText(문자열) -> null이거나 공백만 있거나 빈 문자열이면 false
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
















