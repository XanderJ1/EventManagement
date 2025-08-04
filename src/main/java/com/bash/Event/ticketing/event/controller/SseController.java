package com.bash.Event.ticketing.event.controller;

import com.bash.Event.ticketing.event.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(defaultValue = "") String clientId) {
        if (clientId.isEmpty()) {
            clientId = UUID.randomUUID().toString();
        }
        log.info("SSE subscription request from client: {}", clientId);
        return sseService.subscribe(clientId);
    }

    @DeleteMapping("/unsubscribe/{clientId}")
    public void unsubscribe(@PathVariable String clientId) {
        log.info("SSE unsubscribe request from client: {}", clientId);
        sseService.removeClient(clientId);
    }
}