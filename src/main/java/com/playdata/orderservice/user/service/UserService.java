package com.playdata.orderservice.user.service;

import com.playdata.orderservice.common.auth.TokenUserInfo;
import com.playdata.orderservice.user.dto.UserLoginReqDto;
import com.playdata.orderservice.user.dto.UserResDto;
import com.playdata.orderservice.user.dto.UserSaveReqDto;
import com.playdata.orderservice.user.entity.User;
import com.playdata.orderservice.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public User userCreate(UserSaveReqDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다!");
        }
        User saved = userRepository.save(dto.toEntity(encoder));
        log.info("saved: {}", saved);
        return saved;
    }

    public User login(UserLoginReqDto dto) {
        // 이메일로 user 조회하기
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() ->
                new EntityNotFoundException("User not found")
        );

        // 비밀번호 확인하기 (암호화 되어있으니 encoder에게 부탁)
        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }

    public UserResDto myinfo() {
        TokenUserInfo userInfo
                // 필터에서 세팅한 토큰 정보를 불러오는 메서드 (SecurityContextHolder)
                = (TokenUserInfo) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByEmail(userInfo.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("User not found")
        );

        return user.fromEntity();
    }

    public List<UserResDto> userList(Pageable pageable) {
        // UserResDto가 여러개 리턴되어야 함.
        // 페이징 처리좀 해주세요. 1페이지 요청, 한 화면에 보여줄 회원 수: 6명

//        Pageable pageable = PageRequest.of(0, 6); -> 이게 필요가 없어짐. 컨트롤러가 넘겨줌.
        Page<User> users = userRepository.findAll(pageable);

        // 실질적 데이터(userList)
        List<User> content = users.getContent();
        List<UserResDto> dtoList = content.stream()
                .map(user -> user.fromEntity())
                .collect(Collectors.toList());

        // 총 페이지 수
        int totalPages = users.getTotalPages();
        // 총 데이터 수
        long total = users.getTotalElements();

        return dtoList;

    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("User not found")
        );
    }
}

















