package com.bash.Event.ticketing.event.service.impl;

import com.bash.Event.ticketing.event.service.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseServiceImpl implements SseService {

    private final Map<String, SseEmitter> clients = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public SseEmitter subscribe(String clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for client: {}", clientId);
            clients.remove(clientId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE connection timed out for client: {}", clientId);
            clients.remove(clientId);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE connection error for client: {}", clientId, ex);
            clients.remove(clientId);
        });

        clients.put(clientId, emitter);
        log.info("New SSE client connected: {}", clientId);
        
        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("Connected successfully"));
        } catch (IOException e) {
            log.error("Error sending initial message to client: {}", clientId, e);
            clients.remove(clientId);
        }
        
        return emitter;
    }

    @Override
    public void sendEventUpdate(UUID eventId, String eventType, Object data) {
        Map<String, Object> message = Map.of(
                "type", "event_update",
                "eventId", eventId,
                "eventType", eventType,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        broadcastMessage("event_update", message);
    }

    @Override
    public void sendTicketUpdate(UUID ticketId, String eventType, Object data) {
        Map<String, Object> message = Map.of(
                "type", "ticket_update",
                "ticketId", ticketId,
                "eventType", eventType,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        broadcastMessage("ticket_update", message);
    }

    @Override
    public void sendDashboardUpdate(Object data) {
        Map<String, Object> message = Map.of(
                "type", "dashboard_update",
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        broadcastMessage("dashboard_update", message);
    }

    @Override
    public void removeClient(String clientId) {
        SseEmitter emitter = clients.remove(clientId);
        if (emitter != null) {
            emitter.complete();
            log.info("Client removed: {}", clientId);
        }
    }

    private void broadcastMessage(String eventName, Object data) {
        clients.entrySet().removeIf(entry -> {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name(eventName)
                        .data(objectMapper.writeValueAsString(data)));
                return false;
            } catch (IOException e) {
                log.error("Error sending message to client: {}", entry.getKey(), e);
                return true; // Remove client on error
            }
        });
    }
}