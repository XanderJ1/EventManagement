package com.bash.Event.ticketing.event.service;


import com.bash.Event.ticketing.event.dto.request.EventRequest;
import com.bash.Event.ticketing.event.dto.response.EventResponse;
import com.bash.Event.ticketing.event.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventService {

    MessageResponse<EventResponse> createEvent(EventRequest eventRequest);

    MessageResponse<EventResponse> updateEvent(UUID eventId, EventRequest eventRequest);

    MessageResponse<EventResponse> getEventById(UUID eventId);

    MessageResponse<Void> deleteEvent(UUID eventId);

    MessageResponse<Page<EventResponse>> getAllEvents(Pageable pageable);

    MessageResponse<Page<EventResponse>> getUserEvents(Pageable pageable);
}
