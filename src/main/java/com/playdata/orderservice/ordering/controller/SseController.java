package com.playdata.orderservice.ordering.controller;

import com.playdata.orderservice.common.auth.TokenUserInfo;
import com.playdata.orderservice.ordering.dto.OrderingListResDto;
import com.playdata.orderservice.ordering.entity.Ordering;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class SseController {

    // 구독을 요청한 각 사용자의 이메일을 키로 하여 emitter 객체를 저장.
    // ConcurrentHashMap: 멀티 스레드 기반 해시맵 (HashMap은 싱글 스레드 기반)
    Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/subscribe")
    public SseEmitter subscribe(@AuthenticationPrincipal TokenUserInfo userInfo) {
        SseEmitter emitter = new SseEmitter(1440 * 60 * 1000L); // 알림 서비스 구현 핵심 객체.
        String email = userInfo.getEmail();
        emitters.put(email, emitter); // 이메일을 키로 emitter 저장

        log.info("Subscribing to {}", email);

        // 클라이언트가 연결을 끊거나, emitter의 수명이 다하면 맵에서 제거.
        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));

        // 연결 성공 메세지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!!!"));

            // 30초마다 heartbeat 메시지를 전송하여 연결 유지
            // 클라이언트에서 사용하는 EventSourcePolyfill이 45초 동안 활동이 없으면 지맘대로 연결 종료
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("keep-alive")); // 클라이언트 단이 살아있는지 확인
                } catch (IOException e) {
                    emitters.remove(email);
                    System.out.println("Failed to send heartbeat, removing emitter for email: " + email);
                }
            }, 30, 30, TimeUnit.SECONDS); // 30초마다 heartbeat 메시지 전송


        } catch (IOException e) {
            emitters.remove(email);
        }

        return emitter;
    }

    public void sendOrderMessage(Ordering save) {
        OrderingListResDto dto = save.fromEntity();
        // 누구에게 메세지를 전달할 지 알려줘야 한다. (admin@admin.com이 받는다고 가정)
        SseEmitter emitter = emitters.get("admin@admin.com");
        try {
            emitter.send(SseEmitter.event()
                    .name("ordered")
                    .data(dto));
        } catch (IOException e) {
            emitters.remove("admin@admin.com");
        }

    }



}












