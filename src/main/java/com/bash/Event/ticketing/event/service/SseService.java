package com.bash.Event.ticketing.event.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface SseService {

    SseEmitter subscribe(String clientId);
    void sendEventUpdate(UUID eventId, String eventType, Object data);
    void sendTicketUpdate(UUID eventId, String eventType, Object data);
    void sendDashboardUpdate(Object data);
    void removeClient(String clientId);
}
