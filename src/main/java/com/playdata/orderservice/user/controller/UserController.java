package com.playdata.orderservice.user.controller;

import com.playdata.orderservice.common.auth.JwtTokenProvider;
import com.playdata.orderservice.common.auth.TokenUserInfo;
import com.playdata.orderservice.common.dto.CommonErrorDto;
import com.playdata.orderservice.common.dto.CommonResDto;
import com.playdata.orderservice.user.dto.UserLoginReqDto;
import com.playdata.orderservice.user.dto.UserResDto;
import com.playdata.orderservice.user.dto.UserSaveReqDto;
import com.playdata.orderservice.user.entity.Role;
import com.playdata.orderservice.user.entity.User;
import com.playdata.orderservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Qualifier("user-template") // RedisTemplate이 여러 개 빈 등록되었을 경우 명시한다.
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/create")
    public ResponseEntity<?> userCreate(@Valid @RequestBody UserSaveReqDto dto) {
        log.info("/user/create: POST, dto: {}", dto);
        User user = userService.userCreate(dto);

        CommonResDto resDto
                = new CommonResDto(HttpStatus.CREATED, "member create 성공", user.getId());
        return new ResponseEntity<>(resDto, HttpStatus.CREATED);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody UserLoginReqDto dto) {
        // email, password가 맞는 지 검증
        User user = userService.login(dto);

        // 회원 정보가 일치한다면, JWT를 클라이언트에게 발급해 주어야 한다. -> 로그인 유지를 위해!
        // Access Token을 생성해서 발급해 주겠다. -> 수명이 짧습니다.
        String token
                = jwtTokenProvider.createToken(user.getEmail(), user.getRole().toString());
        log.info("token: {}", token);

        // Refresh Token을 생성해 주겠다.
        // Access Token의 수명이 만료되었을 경우 Refresh Token을 확인해서 리프레시가 유효한 경우
        // 로그인 없이 Access Token을 재발급 해주는 용도로 사용.
        String refreshToken
                = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().toString());

        // refresh Token을 DB에 저장하자. -> redis에 저장.
        redisTemplate.opsForValue().set(user.getEmail(), refreshToken, 240, TimeUnit.HOURS);


        // 생성된 토큰 외에 추가로 전달할 정보가 있다면 Map을 사용하는 것이 좋습니다.
        Map<String, Object> logInfo = new HashMap<>();
        logInfo.put("token", token);
        logInfo.put("id", user.getId());

        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK, "로그인 성공!", logInfo);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // 회원 정보 조회 (관리자) -> ADMIN만 회원 목록 전체를 조회할 수 있다.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    // 컨트롤러 파라미터로 Pageable 선언하면, 페이징 파라미터 처리를 쉽게 진행할 수 있음.
    // /list?number=1&size=10&sort=name,desc 요런 식으로.
    // 요청 시 쿼리스트링이 전달되지 않으면 기본값 0, 20, unsorted
    public ResponseEntity<?> userList(Pageable pageable) {
        log.info("/user/list: GET!!");
        log.info("pageable: {}", pageable);

        List<UserResDto> userResDtos = userService.userList(pageable);
        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK, "userList 조회 성공", userResDtos);
        return ResponseEntity.ok().body(resDto);
    }


    // 회원 정보 조회 (마이페이지) -> 일반 회원이 요청합니다.
    @GetMapping("/myinfo")
    public ResponseEntity<?> myInfo() {
        UserResDto dto = userService.myinfo();
        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK, "myInfo 조회 성공", dto);

        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // access token이 만료되어 새 토큰을 요청
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> id) {
        log.info("/user/refresh: POST, id: {}", id);
            User user = userService.findById(Long.parseLong(id.get("id")));
        log.info("조회된 User: {}", user);

        // email로 redis를 조회해서 refresh token을 가져오자
        Object obj = redisTemplate.opsForValue().get(user.getEmail());
        log.info("레디스에서 조회한 데이터: {}", obj);
        if (obj == null) { // refresh token의 수명이 다됨.
            log.info("refresh 만료!");
            return new ResponseEntity<>(new CommonErrorDto(
                    HttpStatus.UNAUTHORIZED,
                    "EXPIRED_RT"
            ), HttpStatus.UNAUTHORIZED);
        }

        // 새로운 access token을 발급하자.
        String newAccessToken
                = jwtTokenProvider.createToken(user.getEmail(), user.getRole().toString());

        Map<String, Object> info = new HashMap<>();
        info.put("token", newAccessToken);
        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK, "새 토큰 발급됨!", info);

        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }






}



















