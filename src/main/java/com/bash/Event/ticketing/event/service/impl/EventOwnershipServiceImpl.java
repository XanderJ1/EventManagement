package com.bash.Event.ticketing.event.service.impl;

import com.bash.Event.ticketing.event.repository.EventRepository;
import com.bash.Event.ticketing.event.service.EventOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventOwnershipServiceImpl implements EventOwnershipService {

    private final EventRepository eventRepository;

    @Override
    public boolean isEventOwner(UUID eventId, String userEmail) {
        return eventRepository.findById(eventId)
                .map(event -> event.getCreatedBy() != null && event.getCreatedBy().equals(userEmail))
                .orElse(false);
    }

    @Override
    public void validateEventOwnership(UUID eventId, String userEmail) {
        if (!isEventOwner(eventId, userEmail)) {
            log.warn("User {} attempted to access event {} without ownership", userEmail, eventId);
            throw new AccessDeniedException("You don't have permission to access this event");
        }
    }
}