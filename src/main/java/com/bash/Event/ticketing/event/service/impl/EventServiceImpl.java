package com.bash.Event.ticketing.event.service.impl;

import com.bash.Event.ticketing.event.dto.request.EventRequest;
import com.bash.Event.ticketing.event.dto.response.EventResponse;
import com.bash.Event.ticketing.event.dto.response.MessageResponse;
import com.bash.Event.ticketing.event.mappers.EventMapper;
import com.bash.Event.ticketing.event.model.Event;
import com.bash.Event.ticketing.event.repository.EventRepository;
import com.bash.Event.ticketing.event.service.EventOwnershipService;
import com.bash.Event.ticketing.event.service.EventService;
import com.bash.Event.ticketing.event.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final SseService sseService;
    private final EventOwnershipService eventOwnershipService;

    @Override
    @CacheEvict(value = {"events", "dashboard"}, allEntries = true)
    public MessageResponse<EventResponse> createEvent(EventRequest eventRequest) {
        log.info("Creating event with request: {}", eventRequest);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Current user authentication: {}", authentication);
        String userEmail = authentication.getName();
        
        if (eventRepository.existsByTitleIgnoreCaseAndStartTime(eventRequest.getEventName(),
                eventRequest.getStartTime())) {
            return new MessageResponse<>("Event with the same title and start time already exists.");
        }
        
        Event event = eventMapper.mapToEvent(eventRequest);
        event.setCreatedBy(userEmail);

        EventResponse eventResponse = eventMapper.mapToEventResponse(eventRepository.save(event));
        log.info("Event created successfully by user {}: {}", userEmail, eventResponse);
        
        sseService.sendEventUpdate(eventResponse.getEventId(), "CREATED", eventResponse);
        
        return MessageResponse.success("Event Created Successfully", eventResponse);
    }

    @Override
    @CacheEvict(value = {"events", "dashboard"}, allEntries = true)
    public MessageResponse<EventResponse> updateEvent(UUID eventId, EventRequest eventRequest) {
        log.info("Updating event with ID: {} and request: {}", eventId, eventRequest);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        
        eventOwnershipService.validateEventOwnership(eventId, userEmail);
        
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));

        Event updatedEvent = eventMapper.mapToEvent(eventRequest);

        if(updatedEvent.getTitle() != null){
            existingEvent.setTitle(updatedEvent.getTitle());
        }
        if(updatedEvent.getDescription() != null){
            existingEvent.setDescription(updatedEvent.getDescription());
        }
        if(updatedEvent.getStartTime() != null){
            existingEvent.setStartTime(updatedEvent.getStartTime());
        }
        if(updatedEvent.getEndTime() != null){
            existingEvent.setEndTime(updatedEvent.getEndTime());
        }
        if(updatedEvent.getVenue()!=null){
            existingEvent.setVenue(updatedEvent.getVenue());
        }

        EventResponse eventResponse = eventMapper.mapToEventResponse(eventRepository.save(existingEvent));
        log.info("Event updated successfully by user {}: {}", userEmail, eventResponse);
        
        // Send SSE notification
        sseService.sendEventUpdate(eventId, "UPDATED", eventResponse);
        
        return MessageResponse.success("Event Updated Successfully", eventResponse);
    }

    @Override
    @Cacheable(value = "events", key = "#eventId")
    public MessageResponse<EventResponse> getEventById(UUID eventId) {
        log.info("Retrieving event with ID: {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
        EventResponse eventResponse = eventMapper.mapToEventResponse(event);
        log.info("Event retrieved successfully: {}", eventResponse);
        return MessageResponse.success("Event Retrieved Successfully", eventResponse);
    }

    @Override
    @CacheEvict(value = {"events", "dashboard"}, allEntries = true)
    public MessageResponse<Void> deleteEvent(UUID eventId) {
        log.info("Deleting event with ID: {}", eventId);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        
        eventOwnershipService.validateEventOwnership(eventId, userEmail);
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
        
        eventRepository.delete(event);
        log.info("Event deleted successfully by user {}: {}", userEmail, eventId);
        
        sseService.sendEventUpdate(eventId, "DELETED", null);
        
        return MessageResponse.success("Event Deleted Successfully", null);
    }

    @Override
    @Cacheable(value = "events", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public MessageResponse<Page<EventResponse>> getAllEvents(Pageable pageable) {
        log.info("Retrieving all events");
        Page<Event> events = eventRepository.findAll(pageable);
        if (events.isEmpty()) {
            log.warn("No events found");
            return MessageResponse.success("No events found", Page.empty());
        }
        Page<EventResponse> eventResponses = new PageImpl<>(
                events.stream()
                        .map(eventMapper::mapToEventResponse)
                        .collect(Collectors.toList()),
                pageable,
                events.getTotalElements()
        );

        return MessageResponse.success("Events Retrieved Successfully", eventResponses);
    }

    public MessageResponse<Page<EventResponse>> getUserEvents(Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Retrieving user events for authentication: {}", authentication);

        log.info("Current user authentication: {}", authentication);
        String userEmail = authentication.getName();
        
        log.info("Retrieving events for user: {}", userEmail);
        Page<Event> events = eventRepository.findByCreatedBy(userEmail, pageable);
        
        if (events.isEmpty()) {
            log.warn("No events found for user: {}", userEmail);
            return MessageResponse.success("No events found", Page.empty());
        }
        
        Page<EventResponse> eventResponses = new PageImpl<>(
                events.stream()
                        .map(eventMapper::mapToEventResponse)
                        .collect(Collectors.toList()),
                pageable,
                events.getTotalElements()
        );

        return MessageResponse.success("User Events Retrieved Successfully", eventResponses);
    }

}
