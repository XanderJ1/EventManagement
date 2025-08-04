package com.bash.Event.ticketing.event.service;

import java.util.UUID;

public interface EventOwnershipService {
    boolean isEventOwner(UUID eventId, String userEmail);
    void validateEventOwnership(UUID eventId, String userEmail);
}
