package com.bash.Event.ticketing.event.service.impl;

import com.bash.Event.ticketing.event.dto.request.EventRequest;
import com.bash.Event.ticketing.event.dto.response.EventResponse;
import com.bash.Event.ticketing.event.dto.response.MessageResponse;
import com.bash.Event.ticketing.event.mappers.EventMapper;
import com.bash.Event.ticketing.event.model.Address;
import com.bash.Event.ticketing.event.model.Event;
import com.bash.Event.ticketing.event.repository.EventRepository;
import com.bash.Event.ticketing.event.service.EventOwnershipService;
import com.bash.Event.ticketing.event.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(10)
public class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private SseService sseService;

    @Mock
    private EventOwnershipService eventOwnershipService;

    private EventServiceImpl eventService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        eventService = new EventServiceImpl(eventRepository, eventMapper, sseService, eventOwnershipService);
    }

    @Test
    public void testCreateEventSuccessfully() {
        // Arrange
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventName("Test Event");
        eventRequest.setDescription("Test Description");
        eventRequest.setStartTime(LocalDateTime.now());
        eventRequest.setEndTime(LocalDateTime.now().plusHours(2));
        Event mappedEvent = new Event();
        mappedEvent.setTitle("Test Event");
        mappedEvent.setDescription("Test Description");
        mappedEvent.setStartTime(eventRequest.getStartTime());
        mappedEvent.setEndTime(eventRequest.getEndTime());
        Event savedEvent = new Event();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setTitle("Test Event");
        savedEvent.setDescription("Test Description");
        savedEvent.setCreatedBy("test@example.com");
        EventResponse eventResponse = new EventResponse();
        eventResponse.setEventId(savedEvent.getId());
        eventResponse.setTitle("Test Event");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test@example.com");
            when(eventRepository.existsByTitleIgnoreCaseAndStartTime(anyString(), any(LocalDateTime.class))).thenReturn(false);
            when(eventMapper.mapToEvent(eventRequest)).thenReturn(mappedEvent);
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(eventMapper.mapToEventResponse(savedEvent)).thenReturn(eventResponse);
            doNothing().when(sseService).sendEventUpdate(any(UUID.class), anyString(), any());
            // Act
            MessageResponse<EventResponse> result = eventService.createEvent(eventRequest);
            // Assert
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("Event Created Successfully", result.getMessage());
            assertNotNull(result.getData());
            verify(eventRepository, atLeast(1)).save(any(Event.class));
            verify(sseService, atLeast(1)).sendEventUpdate(any(UUID.class), eq("CREATED"), any());
        }
    }

    @Test
    public void testCreateEventWhenEventAlreadyExists() {
        // Arrange
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventName("Existing Event");
        eventRequest.setStartTime(LocalDateTime.now());
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test@example.com");
            when(eventRepository.existsByTitleIgnoreCaseAndStartTime(eventRequest.getEventName(), eventRequest.getStartTime())).thenReturn(true);
            // Act
            MessageResponse<EventResponse> result = eventService.createEvent(eventRequest);
            // Assert
            assertNotNull(result);
            assertEquals("Event with the same title and start time already exists.", result.getMessage());
        }
    }

    @Test
    public void testUpdateEventSuccessfully() {
        // Arrange
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventName("Updated Event");
        eventRequest.setDescription("Updated Description");
        Event existingEvent = new Event();
        existingEvent.setId(eventId);
        existingEvent.setTitle("Original Event");
        existingEvent.setDescription("Original Description");
        Event updatedMappedEvent = new Event();
        updatedMappedEvent.setTitle("Updated Event");
        updatedMappedEvent.setDescription("Updated Description");
        EventResponse eventResponse = new EventResponse();
        eventResponse.setEventId(eventId);
        eventResponse.setTitle("Updated Event");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test@example.com");
            doNothing().when(eventOwnershipService).validateEventOwnership(eventId, "test@example.com");
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventMapper.mapToEvent(eventRequest)).thenReturn(updatedMappedEvent);
            when(eventRepository.save(existingEvent)).thenReturn(existingEvent);
            when(eventMapper.mapToEventResponse(existingEvent)).thenReturn(eventResponse);
            doNothing().when(sseService).sendEventUpdate(eventId, "UPDATED", eventResponse);
            // Act
            MessageResponse<EventResponse> result = eventService.updateEvent(eventId, eventRequest);
            // Assert
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("Event Updated Successfully", result.getMessage());
            assertNotNull(result.getData());
            verify(eventOwnershipService, atLeast(1)).validateEventOwnership(eventId, "test@example.com");
            verify(eventRepository, atLeast(1)).save(existingEvent);
            verify(sseService, atLeast(1)).sendEventUpdate(eventId, "UPDATED", eventResponse);
        }
    }

    @Test
    public void testUpdateEventWhenEventNotFound() {
        // Arrange
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        EventRequest eventRequest = new EventRequest();
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test@example.com");
            doNothing().when(eventOwnershipService).validateEventOwnership(eventId, "test@example.com");
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                eventService.updateEvent(eventId, eventRequest);
            });
            assertEquals("Event not found with ID: " + eventId, exception.getMessage());
        }
    }

    @Test
    public void testUpdateEventWithPartialFields() {
        // Arrange
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventName("Updated Event");
        Event existingEvent = new Event();
        existingEvent.setId(eventId);
        existingEvent.setTitle("Original Event");
        existingEvent.setDescription("Original Description");
        Event updatedMappedEvent = new Event();
        updatedMappedEvent.setTitle("Updated Event");
        updatedMappedEvent.setDescription(null);
        updatedMappedEvent.setStartTime(null);
        updatedMappedEvent.setEndTime(null);
        updatedMappedEvent.setVenue(null);
        EventResponse eventResponse = new EventResponse();
        eventResponse.setEventId(eventId);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test@example.com");
            doNothing().when(eventOwnershipService).validateEventOwnership(eventId, "test@example.com");
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventMapper.mapToEvent(eventRequest)).thenReturn(updatedMappedEvent);
            when(eventRepository.save(existingEvent)).thenReturn(existingEvent);
            when(eventMapper.mapToEventResponse(existingEvent)).thenReturn(eventResponse);
            doNothing().when(sseService).sendEventUpdate(eventId, "UPDATED", eventResponse);
            // Act
            MessageResponse<EventResponse> result = eventService.updateEvent(eventId, eventRequest);
            // Assert
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("Event Updated Successfully", result.getMessage());
            assertThat(existingEvent.getTitle(), is(equalTo("Updated Event")));
            assertThat(existingEvent.getDescription(), is(equalTo("Original Description")));
        }
    }

    @Test
    public void testUpdateEventWithAllFields() {
        // Arrange
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventName("Updated Event");
        eventRequest.setDescription("Updated Description");
        LocalDateTime newStartTime = LocalDateTime.now().plusDays(1);
        LocalDateTime newEndTime = LocalDateTime.now().plusDays(1).plusHours(2);
        Event existingEvent = new Event();
        existingEvent.setId(eventId);
        existingEvent.setTitle("Original Event");
        existingEvent.setDescription("Original Description");
        Event updatedMappedEvent = new Event();
        updatedMappedEvent.setTitle("Updated Event");
        updatedMappedEvent.setDescription("Updated Description");
        updatedMappedEvent.setStartTime(newStartTime);
        updatedMappedEvent.setEndTime(newEndTime);
        updatedMappedEvent.setVenue(new Address("New Venue", "New Street", "New City", "New Country"));
        EventResponse eventResponse = new EventResponse();
        eventResponse.setEventId(eventId);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test@example.com");
            doNothing().when(eventOwnershipService).validateEventOwnership(eventId, "test@example.com");
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
            when(eventMapper.mapToEvent(eventRequest)).thenReturn(updatedMappedEvent);
            when(eventRepository.save(existingEvent)).thenReturn(existingEvent);
            when(eventMapper.mapToEventResponse(existingEvent)).thenReturn(eventResponse);
            doNothing().when(sseService).sendEventUpdate(eventId, "UPDATED", eventResponse);
            // Act
            MessageResponse<EventResponse> result = eventService.updateEvent(eventId, eventRequest);
            // Assert
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("Event Updated Successfully", result.getMessage());
            assertThat(existingEvent.getTitle(), is(equalTo("Updated Event")));
            assertThat(existingEvent.getDescription(), is(equalTo("Updated Description")));
            assertThat(existingEvent.getStartTime(), is(equalTo(newStartTime)));
            assertThat(existingEvent.getEndTime(), is(equalTo(newEndTime)));
            assertNotNull(existingEvent.getVenue());
        }
    }

    @Test
    public void testGetEventByIdSuccessfully() {
        // Arrange
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Event event = new Event();
        event.setId(eventId);
        event.setTitle("Test Event");
        EventResponse eventResponse = new EventResponse();
        eventResponse.setEventId(eventId);
        eventResponse.setTitle("Test Event");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventMapper.mapToEventResponse(event)).thenReturn(eventResponse);
        // Act
        MessageResponse<EventResponse> result = eventService.getEventById(eventId);
        // Assert
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("Event Retrieved Successfully", result.getMessage());
        assertNotNull(result.getData());
        assertThat(result.getData().getEventId(), is(equalTo(eventId)));
        verify(eventRepository, atLeast(1)).findById(eventId);
        verify(eventMapper, atLeast(1)).mapToEventResponse(event);
    }

    @Test
    public void testGetEventByIdWhenEventNotFound() {
        // Arrange
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.getEventById(eventId);
        });
        assertEquals("Event not found with ID: " + eventId, exception.getMessage());
        verify(eventRepository, atLeast(1)).findById(eventId);
    }

    @Test
    public void testDeleteEventSuccessfully() {
        // Arrange
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Event event = new Event();
        event.setId(eventId);
        event.setTitle("Test Event");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test@example.com");
            doNothing().when(eventOwnershipService).validateEventOwnership(eventId, "test@example.com");
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            doNothing().when(eventRepository).delete(event);
            doNothing().when(sseService).sendEventUpdate(eventId, "DELETED", null);
            // Act
            MessageResponse<Void> result = eventService.deleteEvent(eventId);
            // Assert
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("Event Deleted Successfully", result.getMessage());
            verify(eventOwnershipService, atLeast(1)).validateEventOwnership(eventId, "test@example.com");
            verify(eventRepository, atLeast(1)).findById(eventId);
            verify(eventRepository, atLeast(1)).delete(event);
            verify(sseService, atLeast(1)).sendEventUpdate(eventId, "DELETED", null);
        }
    }

    @Test
    public void testDeleteEventWhenEventNotFound() {
        // Arrange
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("test@example.com");
            doNothing().when(eventOwnershipService).validateEventOwnership(eventId, "test@example.com");
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                eventService.deleteEvent(eventId);
            });
            assertEquals("Event not found with ID: " + eventId, exception.getMessage());
            verify(eventOwnershipService, atLeast(1)).validateEventOwnership(eventId, "test@example.com");
            verify(eventRepository, atLeast(1)).findById(eventId);
        }
    }

    @Test
    public void testGetAllEventsSuccessfully() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Event event1 = new Event();
        event1.setId(UUID.randomUUID());
        event1.setTitle("Event 1");
        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setTitle("Event 2");
        List<Event> events = List.of(event1, event2);
        Page<Event> eventPage = new PageImpl<>(events, pageable, events.size());
        EventResponse eventResponse1 = new EventResponse();
        eventResponse1.setEventId(event1.getId());
        eventResponse1.setTitle("Event 1");
        EventResponse eventResponse2 = new EventResponse();
        eventResponse2.setEventId(event2.getId());
        eventResponse2.setTitle("Event 2");
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);
        when(eventMapper.mapToEventResponse(event1)).thenReturn(eventResponse1);
        when(eventMapper.mapToEventResponse(event2)).thenReturn(eventResponse2);
        // Act
        MessageResponse<Page<EventResponse>> result = eventService.getAllEvents(pageable);
        // Assert
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("Events Retrieved Successfully", result.getMessage());
        assertNotNull(result.getData());
        assertThat(result.getData().getContent().size(), is(equalTo(2)));
        verify(eventRepository, atLeast(1)).findAll(pageable);
    }

    @Test
    public void testGetAllEventsWhenNoEventsFound() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(eventRepository.findAll(pageable)).thenReturn(emptyPage);
        // Act
        MessageResponse<Page<EventResponse>> result = eventService.getAllEvents(pageable);
        // Assert
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("No events found", result.getMessage());
        assertNotNull(result.getData());
        assertThat(result.getData().getContent().size(), is(equalTo(0)));
        verify(eventRepository, atLeast(1)).findAll(pageable);
    }

    @Test
    public void testGetUserEventsSuccessfully() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        String userEmail = "test@example.com";
        Event event1 = new Event();
        event1.setId(UUID.randomUUID());
        event1.setTitle("User Event 1");
        event1.setCreatedBy(userEmail);
        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setTitle("User Event 2");
        event2.setCreatedBy(userEmail);
        List<Event> events = List.of(event1, event2);
        Page<Event> eventPage = new PageImpl<>(events, pageable, events.size());
        EventResponse eventResponse1 = new EventResponse();
        eventResponse1.setEventId(event1.getId());
        eventResponse1.setTitle("User Event 1");
        EventResponse eventResponse2 = new EventResponse();
        eventResponse2.setEventId(event2.getId());
        eventResponse2.setTitle("User Event 2");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userEmail);
            when(eventRepository.findByCreatedBy(userEmail, pageable)).thenReturn(eventPage);
            when(eventMapper.mapToEventResponse(event1)).thenReturn(eventResponse1);
            when(eventMapper.mapToEventResponse(event2)).thenReturn(eventResponse2);
            // Act
            MessageResponse<Page<EventResponse>> result = eventService.getUserEvents(pageable);
            // Assert
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("User Events Retrieved Successfully", result.getMessage());
            assertNotNull(result.getData());
            assertThat(result.getData().getContent().size(), is(equalTo(2)));
            verify(eventRepository, atLeast(1)).findByCreatedBy(userEmail, pageable);
        }
    }

    @Test
    public void testGetUserEventsWhenNoEventsFound() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        String userEmail = "test@example.com";
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userEmail);
            when(eventRepository.findByCreatedBy(userEmail, pageable)).thenReturn(emptyPage);
            // Act
            MessageResponse<Page<EventResponse>> result = eventService.getUserEvents(pageable);
            // Assert
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("No events found", result.getMessage());
            assertNotNull(result.getData());
            assertThat(result.getData().getContent().size(), is(equalTo(0)));
            verify(eventRepository, atLeast(1)).findByCreatedBy(userEmail, pageable);
        }
    }

    @Test
    public void testEventServiceImplInstantiation() {
        // Act
        EventServiceImpl service = new EventServiceImpl(eventRepository, eventMapper, sseService, eventOwnershipService);
        // Assert
        assertThat(service, is(notNullValue()));
    }
}
