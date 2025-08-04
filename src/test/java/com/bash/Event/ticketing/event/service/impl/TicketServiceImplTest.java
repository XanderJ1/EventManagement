package com.bash.Event.ticketing.event.service.impl;

import com.bash.Event.ticketing.event.dto.request.PurchaseRequest;
import com.bash.Event.ticketing.event.dto.request.TicketRequest;
import com.bash.Event.ticketing.event.dto.response.DashboardInsights;
import com.bash.Event.ticketing.event.dto.response.MessageResponse;
import com.bash.Event.ticketing.event.dto.response.TicketResponse;
import com.bash.Event.ticketing.event.enums.AttendanceStatus;
import com.bash.Event.ticketing.event.model.Event;
import com.bash.Event.ticketing.event.model.Ticket;
import com.bash.Event.ticketing.event.repository.EventRepository;
import com.bash.Event.ticketing.event.repository.TicketRepository;
import com.bash.Event.ticketing.event.service.EventOwnershipService;
import com.bash.Event.ticketing.event.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeast;

@Timeout(10)
public class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SseService sseService;

    @Mock
    private EventOwnershipService eventOwnershipService;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = spy(new TicketServiceImpl(ticketRepository, eventRepository, sseService, eventOwnershipService));
    }

    @Test
    void testCreateTicketSuccess() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String userEmail = "test@example.com";
        TicketRequest request = new TicketRequest();
        request.setTicketType("VIP");
        request.setPrice(100.0);
        request.setQuantityAvailable(50);
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn(eventId);
        Ticket mockTicket = mock(Ticket.class);
        when(mockTicket.getId()).thenReturn(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"));
        when(mockTicket.getEvent()).thenReturn(mockEvent);
        when(mockTicket.getTicketType()).thenReturn("VIP");
        when(mockTicket.getPrice()).thenReturn(100.0);
        when(mockTicket.getQuantityAvailable()).thenReturn(50);
        when(mockTicket.getQuantitySold()).thenReturn(0);
        when(mockTicket.getAttendanceStatus()).thenReturn(AttendanceStatus.NOT_ATTENDED);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userEmail);
            doNothing().when(eventOwnershipService).validateEventOwnership(eq(eventId), eq(userEmail));
            when(eventRepository.findById(eq(eventId))).thenReturn(Optional.of(mockEvent));
            when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
            doNothing().when(sseService).sendTicketUpdate(any(UUID.class), anyString(), any(TicketResponse.class));
            MessageResponse<TicketResponse> result = ticketService.createTicket(eventId, request);
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("Ticket created successfully", result.getMessage());
            assertNotNull(result.getData());
            assertEquals("VIP", result.getData().getTicketType());
            assertEquals(100.0, result.getData().getPrice(), 0.00001);
            verify(eventOwnershipService, atLeast(1)).validateEventOwnership(eq(eventId), eq(userEmail));
            verify(eventRepository, atLeast(1)).findById(eq(eventId));
            verify(ticketRepository, atLeast(1)).save(any(Ticket.class));
            verify(sseService, atLeast(1)).sendTicketUpdate(any(UUID.class), eq("CREATED"), any(TicketResponse.class));
        }
    }

    @Test
    void testCreateTicketEventNotFound() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String userEmail = "test@example.com";
        TicketRequest request = new TicketRequest();
        request.setTicketType("VIP");
        request.setPrice(100.0);
        request.setQuantityAvailable(50);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userEmail);
            doNothing().when(eventOwnershipService).validateEventOwnership(eq(eventId), eq(userEmail));
            when(eventRepository.findById(eq(eventId))).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> ticketService.createTicket(eventId, request));
            verify(eventOwnershipService, atLeast(1)).validateEventOwnership(eq(eventId), eq(userEmail));
            verify(eventRepository, atLeast(1)).findById(eq(eventId));
        }
    }

    @Test
    void testGetEventTicketsSuccess() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        List<Ticket> mockTickets = new ArrayList<>();
        Ticket mockTicket = mock(Ticket.class);
        Event mockEvent = mock(Event.class);
        when(mockTicket.getId()).thenReturn(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"));
        when(mockTicket.getEvent()).thenReturn(mockEvent);
        when(mockEvent.getId()).thenReturn(eventId);
        when(mockTicket.getTicketType()).thenReturn("VIP");
        when(mockTicket.getPrice()).thenReturn(100.0);
        when(mockTicket.getQuantityAvailable()).thenReturn(50);
        when(mockTicket.getQuantitySold()).thenReturn(10);
        when(mockTicket.getAttendanceStatus()).thenReturn(AttendanceStatus.NOT_ATTENDED);
        mockTickets.add(mockTicket);
        when(ticketRepository.findByEventId(eq(eventId))).thenReturn(mockTickets);
        MessageResponse<List<TicketResponse>> result = ticketService.getEventTickets(eventId);
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("Tickets retrieved successfully", result.getMessage());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("VIP", result.getData().get(0).getTicketType());
        verify(ticketRepository, atLeast(1)).findByEventId(eq(eventId));
    }

    @Test
    void testGetEventTicketsEmptyList() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(ticketRepository.findByEventId(eq(eventId))).thenReturn(new ArrayList<>());
        MessageResponse<List<TicketResponse>> result = ticketService.getEventTickets(eventId);
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("Tickets retrieved successfully", result.getMessage());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().size());
        verify(ticketRepository, atLeast(1)).findByEventId(eq(eventId));
    }

    @Test
    void testPurchaseTicketSuccess() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID ticketId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        PurchaseRequest request = new PurchaseRequest();
        request.setPurchaserEmail("buyer@example.com");
        request.setQuantity(2);
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn(eventId);
        Ticket mockTicket = mock(Ticket.class);
        when(mockTicket.getId()).thenReturn(ticketId);
        when(mockTicket.getEvent()).thenReturn(mockEvent);
        when(mockTicket.getQuantityAvailable()).thenReturn(50);
        when(mockTicket.getQuantitySold()).thenReturn(10);
        when(mockTicket.getTicketType()).thenReturn("VIP");
        when(mockTicket.getPrice()).thenReturn(100.0);
        when(mockTicket.getAttendanceStatus()).thenReturn(AttendanceStatus.NOT_ATTENDED);
        when(ticketRepository.findById(eq(ticketId))).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        doNothing().when(sseService).sendTicketUpdate(any(UUID.class), anyString(), any(TicketResponse.class));
        doNothing().when(sseService).sendDashboardUpdate(any());
        DashboardInsights mockInsights = mock(DashboardInsights.class);
        MessageResponse<DashboardInsights> mockInsightsResponse = MessageResponse.success("success", mockInsights);
        doReturn(mockInsightsResponse).when(ticketService).getDashboardInsights();
        MessageResponse<TicketResponse> result = ticketService.purchaseTicket(eventId, ticketId, request);
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("Ticket purchased successfully", result.getMessage());
        assertNotNull(result.getData());
        verify(ticketRepository, atLeast(1)).findById(eq(ticketId));
        verify(ticketRepository, atLeast(1)).save(any(Ticket.class));
        verify(sseService, atLeast(1)).sendTicketUpdate(eq(ticketId), eq("PURCHASED"), any(TicketResponse.class));
        verify(sseService, atLeast(1)).sendDashboardUpdate(any());
    }

    @Test
    void testPurchaseTicketNotFound() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID ticketId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        PurchaseRequest request = new PurchaseRequest();
        request.setPurchaserEmail("buyer@example.com");
        request.setQuantity(2);
        when(ticketRepository.findById(eq(ticketId))).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> ticketService.purchaseTicket(eventId, ticketId, request));
        verify(ticketRepository, atLeast(1)).findById(eq(ticketId));
    }

    @Test
    void testPurchaseTicketWrongEvent() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID ticketId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        UUID wrongEventId = UUID.fromString("333e4567-e89b-12d3-a456-426614174002");
        PurchaseRequest request = new PurchaseRequest();
        request.setPurchaserEmail("buyer@example.com");
        request.setQuantity(2);
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn(wrongEventId);
        Ticket mockTicket = mock(Ticket.class);
        when(mockTicket.getEvent()).thenReturn(mockEvent);
        when(ticketRepository.findById(eq(ticketId))).thenReturn(Optional.of(mockTicket));
        assertThrows(RuntimeException.class, () -> ticketService.purchaseTicket(eventId, ticketId, request));
        verify(ticketRepository, atLeast(1)).findById(eq(ticketId));
    }

    @Test
    void testPurchaseTicketSoldOut() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID ticketId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        PurchaseRequest request = new PurchaseRequest();
        request.setPurchaserEmail("buyer@example.com");
        request.setQuantity(2);
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn(eventId);
        Ticket mockTicket = mock(Ticket.class);
        when(mockTicket.getEvent()).thenReturn(mockEvent);
        when(mockTicket.getQuantityAvailable()).thenReturn(10);
        when(mockTicket.getQuantitySold()).thenReturn(10);
        when(ticketRepository.findById(eq(ticketId))).thenReturn(Optional.of(mockTicket));
        MessageResponse<TicketResponse> result = ticketService.purchaseTicket(eventId, ticketId, request);
        assertNotNull(result);
        assertEquals("Ticket sold out", result.getMessage());
        verify(ticketRepository, atLeast(1)).findById(eq(ticketId));
    }

    @Test
    void testScanTicketSuccess() {
        UUID ticketId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String userEmail = "test@example.com";
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn(eventId);
        Ticket mockTicket = mock(Ticket.class);
        when(mockTicket.getId()).thenReturn(ticketId);
        when(mockTicket.getEvent()).thenReturn(mockEvent);
        when(mockTicket.getTicketType()).thenReturn("VIP");
        when(mockTicket.getPrice()).thenReturn(100.0);
        when(mockTicket.getQuantityAvailable()).thenReturn(50);
        when(mockTicket.getQuantitySold()).thenReturn(10);
        when(mockTicket.getAttendanceStatus()).thenReturn(AttendanceStatus.SCANNED);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userEmail);
            when(ticketRepository.findById(eq(ticketId))).thenReturn(Optional.of(mockTicket));
            doNothing().when(eventOwnershipService).validateEventOwnership(eq(eventId), eq(userEmail));
            when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
            doNothing().when(sseService).sendTicketUpdate(any(UUID.class), anyString(), any(TicketResponse.class));
            doNothing().when(sseService).sendDashboardUpdate(any());
            DashboardInsights mockInsights = mock(DashboardInsights.class);
            MessageResponse<DashboardInsights> mockInsightsResponse = MessageResponse.success("success", mockInsights);
            doReturn(mockInsightsResponse).when(ticketService).getDashboardInsights();
            MessageResponse<TicketResponse> result = ticketService.scanTicket(ticketId);
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("Ticket scanned successfully", result.getMessage());
            assertNotNull(result.getData());
            verify(ticketRepository, atLeast(1)).findById(eq(ticketId));
            verify(eventOwnershipService, atLeast(1)).validateEventOwnership(eq(eventId), eq(userEmail));
            verify(ticketRepository, atLeast(1)).save(any(Ticket.class));
            verify(sseService, atLeast(1)).sendTicketUpdate(eq(ticketId), eq("SCANNED"), any(TicketResponse.class));
            verify(sseService, atLeast(1)).sendDashboardUpdate(any());
        }
    }

    @Test
    void testScanTicketNotFound() {
        UUID ticketId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        String userEmail = "test@example.com";
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userEmail);
            when(ticketRepository.findById(eq(ticketId))).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> ticketService.scanTicket(ticketId));
            verify(ticketRepository, atLeast(1)).findById(eq(ticketId));
        }
    }

    @Test
    void testGetDashboardInsightsSuccess() {
        Long totalTicketsSold = 100L;
        Long activeAttendances = 50L;
        Long totalEvents = 10L;
        when(ticketRepository.getTotalTicketsSold()).thenReturn(totalTicketsSold);
        when(ticketRepository.getActiveAttendances()).thenReturn(activeAttendances);
        when(eventRepository.count()).thenReturn(totalEvents);
        MessageResponse<DashboardInsights> result = ticketService.getDashboardInsights();
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("Dashboard insights retrieved", result.getMessage());
        assertNotNull(result.getData());
        assertThat(result.getData().getTotalTicketsSold(), equalTo(100L));
        assertThat(result.getData().getActiveAttendances(), equalTo(50L));
        assertThat(result.getData().getTotalEvents(), equalTo(10L));
        assertThat(result.getData().getTotalRevenue(), equalTo(0.0));
        verify(ticketRepository, atLeast(1)).getTotalTicketsSold();
        verify(ticketRepository, atLeast(1)).getActiveAttendances();
        verify(eventRepository, atLeast(1)).count();
    }

    @Test
    void testGetDashboardInsightsWithNullValues() {
        when(ticketRepository.getTotalTicketsSold()).thenReturn(null);
        when(ticketRepository.getActiveAttendances()).thenReturn(null);
        when(eventRepository.count()).thenReturn(5L);
        MessageResponse<DashboardInsights> result = ticketService.getDashboardInsights();
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("Dashboard insights retrieved", result.getMessage());
        assertNotNull(result.getData());
        assertThat(result.getData().getTotalTicketsSold(), equalTo(0L));
        assertThat(result.getData().getActiveAttendances(), equalTo(0L));
        assertThat(result.getData().getTotalEvents(), equalTo(5L));
        verify(ticketRepository, atLeast(1)).getTotalTicketsSold();
        verify(ticketRepository, atLeast(1)).getActiveAttendances();
        verify(eventRepository, atLeast(1)).count();
    }

    @Test
    void testGetUserDashboardInsightsSuccess() {
        String userEmail = "test@example.com";
        UUID eventId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID eventId2 = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        Event mockEvent1 = mock(Event.class);
        Event mockEvent2 = mock(Event.class);
        when(mockEvent1.getId()).thenReturn(eventId1);
        when(mockEvent2.getId()).thenReturn(eventId2);
        List<Event> userEvents = new ArrayList<>();
        userEvents.add(mockEvent1);
        userEvents.add(mockEvent2);
        List<UUID> eventIds = new ArrayList<>();
        eventIds.add(eventId1);
        eventIds.add(eventId2);
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userEmail);
            when(eventRepository.findByCreatedBy(eq(userEmail))).thenReturn(userEvents);
            when(ticketRepository.getTotalTicketsSoldForEvents(anyList())).thenReturn(75L);
            when(ticketRepository.getActiveAttendancesForEvents(anyList())).thenReturn(25L);
            MessageResponse<DashboardInsights> result = ticketService.getUserDashboardInsights();
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("User dashboard insights retrieved", result.getMessage());
            assertNotNull(result.getData());
            assertThat(result.getData().getTotalTicketsSold(), equalTo(75L));
            assertThat(result.getData().getActiveAttendances(), equalTo(25L));
            assertThat(result.getData().getTotalEvents(), equalTo(2L));
            assertThat(result.getData().getTotalRevenue(), equalTo(0.0));
            verify(eventRepository, atLeast(1)).findByCreatedBy(eq(userEmail));
            verify(ticketRepository, atLeast(1)).getTotalTicketsSoldForEvents(anyList());
            verify(ticketRepository, atLeast(1)).getActiveAttendancesForEvents(anyList());
        }
    }

    @Test
    void testGetUserDashboardInsightsWithNullValues() {
        String userEmail = "test@example.com";
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userEmail);
            when(eventRepository.findByCreatedBy(eq(userEmail))).thenReturn(new ArrayList<>());
            when(ticketRepository.getTotalTicketsSoldForEvents(anyList())).thenReturn(null);
            when(ticketRepository.getActiveAttendancesForEvents(anyList())).thenReturn(null);
            MessageResponse<DashboardInsights> result = ticketService.getUserDashboardInsights();
            assertNotNull(result);
            assertEquals("success", result.getStatus());
            assertEquals("User dashboard insights retrieved", result.getMessage());
            assertNotNull(result.getData());
            assertThat(result.getData().getTotalTicketsSold(), equalTo(0L));
            assertThat(result.getData().getActiveAttendances(), equalTo(0L));
            assertThat(result.getData().getTotalEvents(), equalTo(0L));
            verify(eventRepository, atLeast(1)).findByCreatedBy(eq(userEmail));
            verify(ticketRepository, atLeast(1)).getTotalTicketsSoldForEvents(anyList());
            verify(ticketRepository, atLeast(1)).getActiveAttendancesForEvents(anyList());
        }
    }

    @Test
    void testMapToResponseMethod() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID ticketId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        LocalDateTime purchasedAt = LocalDateTime.now();
        LocalDateTime scannedAt = LocalDateTime.now().plusHours(1);
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn(eventId);
        Ticket mockTicket = mock(Ticket.class);
        when(mockTicket.getId()).thenReturn(ticketId);
        when(mockTicket.getEvent()).thenReturn(mockEvent);
        when(mockTicket.getTicketType()).thenReturn("VIP");
        when(mockTicket.getPrice()).thenReturn(100.0);
        when(mockTicket.getQuantityAvailable()).thenReturn(50);
        when(mockTicket.getQuantitySold()).thenReturn(10);
        when(mockTicket.getAttendanceStatus()).thenReturn(AttendanceStatus.SCANNED);
        when(mockTicket.getPurchasedBy()).thenReturn("buyer@example.com");
        when(mockTicket.getPurchasedAt()).thenReturn(purchasedAt);
        when(mockTicket.getScannedAt()).thenReturn(scannedAt);
        when(ticketRepository.findByEventId(eq(eventId))).thenReturn(List.of(mockTicket));
        MessageResponse<List<TicketResponse>> result = ticketService.getEventTickets(eventId);
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        TicketResponse response = result.getData().get(0);
        assertThat(response.getId(), equalTo(ticketId));
        assertThat(response.getEventId(), equalTo(eventId));
        assertThat(response.getTicketType(), equalTo("VIP"));
        assertThat(response.getPrice(), equalTo(100.0));
        assertThat(response.getQuantityAvailable(), equalTo(50));
        assertThat(response.getQuantitySold(), equalTo(10));
        assertThat(response.getAttendanceStatus(), equalTo(AttendanceStatus.SCANNED));
        assertThat(response.getPurchasedBy(), equalTo("buyer@example.com"));
        assertThat(response.getPurchasedAt(), equalTo(purchasedAt));
        assertThat(response.getScannedAt(), equalTo(scannedAt));
    }

    @Test
    void testMapToResponseWithNullValues() {
        UUID eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID ticketId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn(eventId);
        Ticket mockTicket = mock(Ticket.class);
        when(mockTicket.getId()).thenReturn(ticketId);
        when(mockTicket.getEvent()).thenReturn(mockEvent);
        when(mockTicket.getTicketType()).thenReturn("Standard");
        when(mockTicket.getPrice()).thenReturn(50.0);
        when(mockTicket.getQuantityAvailable()).thenReturn(100);
        when(mockTicket.getQuantitySold()).thenReturn(0);
        when(mockTicket.getAttendanceStatus()).thenReturn(AttendanceStatus.NOT_ATTENDED);
        when(mockTicket.getPurchasedBy()).thenReturn(null);
        when(mockTicket.getPurchasedAt()).thenReturn(null);
        when(mockTicket.getScannedAt()).thenReturn(null);
        when(ticketRepository.findByEventId(eq(eventId))).thenReturn(List.of(mockTicket));
        MessageResponse<List<TicketResponse>> result = ticketService.getEventTickets(eventId);
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        TicketResponse response = result.getData().get(0);
        assertThat(response.getId(), equalTo(ticketId));
        assertThat(response.getEventId(), equalTo(eventId));
        assertThat(response.getTicketType(), equalTo("Standard"));
        assertThat(response.getPrice(), equalTo(50.0));
        assertThat(response.getPurchasedBy(), equalTo(null));
        assertThat(response.getPurchasedAt(), equalTo(null));
        assertThat(response.getScannedAt(), equalTo(null));
    }
}
