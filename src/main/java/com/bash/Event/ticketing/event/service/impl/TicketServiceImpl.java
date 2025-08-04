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
import com.bash.Event.ticketing.event.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final SseService sseService;
    private final EventOwnershipService eventOwnershipService;

    @Override
    @Transactional
    @CacheEvict(value = {"tickets", "dashboard"}, allEntries = true)
    public MessageResponse<TicketResponse> createTicket(UUID eventId, TicketRequest request) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        
        // Validate ownership
        eventOwnershipService.validateEventOwnership(eventId, userEmail);
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Ticket ticket = Ticket.builder()
                .event(event)
                .ticketType(request.getTicketType())
                .price(request.getPrice())
                .quantityAvailable(request.getQuantityAvailable())
                .build();

        Ticket saved = ticketRepository.save(ticket);
        TicketResponse response = mapToResponse(saved);
        
        log.info("Ticket created successfully by user {}: {}", userEmail, response);
        
        // Send SSE notification
        sseService.sendTicketUpdate(saved.getId(), "CREATED", response);
        
        return MessageResponse.success("Ticket created successfully", response);
    }

    @Override
    @Cacheable(value = "tickets", key = "#eventId")
    public MessageResponse<List<TicketResponse>> getEventTickets(UUID eventId) {
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        List<TicketResponse> responses = tickets.stream()
                .map(this::mapToResponse)
                .toList();
        return MessageResponse.success("Tickets retrieved successfully", responses);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"tickets", "dashboard"}, allEntries = true)
    public MessageResponse<TicketResponse> purchaseTicket(UUID eventId, UUID ticketId, PurchaseRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getEvent().getId().equals(eventId)) {
            throw new RuntimeException("Ticket does not belong to this event");
        }

        if (ticket.getQuantityAvailable() <= ticket.getQuantitySold()) {
            return new MessageResponse<>("Ticket sold out");
        }

        ticket.setQuantitySold(ticket.getQuantitySold() + request.getQuantity());
        ticket.setQuantityAvailable(ticket.getQuantityAvailable() - request.getQuantity());
        ticket.setPurchasedBy(request.getPurchaserEmail());
        ticket.setPurchasedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        TicketResponse response = mapToResponse(saved);
        
        // Send SSE notifications
        sseService.sendTicketUpdate(ticketId, "PURCHASED", response);
        sseService.sendDashboardUpdate(getDashboardInsights().getData());
        
        return MessageResponse.success("Ticket purchased successfully", response);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"tickets", "dashboard"}, allEntries = true)
    public MessageResponse<TicketResponse> scanTicket(UUID ticketId) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Validate that user owns the event
        eventOwnershipService.validateEventOwnership(ticket.getEvent().getId(), userEmail);

        ticket.setAttendanceStatus(AttendanceStatus.SCANNED);
        ticket.setScannedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        TicketResponse response = mapToResponse(saved);
        
        log.info("Ticket scanned successfully by user {}: {}", userEmail, response);
        
        // Send SSE notifications
        sseService.sendTicketUpdate(ticketId, "SCANNED", response);
        sseService.sendDashboardUpdate(getDashboardInsights().getData());
        
        return MessageResponse.success("Ticket scanned successfully", response);
    }

    @Override
    @Cacheable(value = "dashboard", key = "'insights'")
    public MessageResponse<DashboardInsights> getDashboardInsights() {
        Long totalTicketsSold = ticketRepository.getTotalTicketsSold();
        Long activeAttendances = ticketRepository.getActiveAttendances();
        Long totalEvents = eventRepository.count();

        DashboardInsights insights = DashboardInsights.builder()
                .totalTicketsSold(totalTicketsSold != null ? totalTicketsSold : 0L)
                .activeAttendances(activeAttendances != null ? activeAttendances : 0L)
                .totalEvents(totalEvents)
                .totalRevenue(0.0)
                .build();

        return MessageResponse.success("Dashboard insights retrieved", insights);
    }

    // Add method to get user's dashboard insights
    public MessageResponse<DashboardInsights> getUserDashboardInsights() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        
        // Get user's events and calculate insights
        List<Event> userEvents = eventRepository.findByCreatedBy(userEmail);
        List<UUID> eventIds = userEvents.stream().map(Event::getId).toList();
        
        Long totalTicketsSold = ticketRepository.getTotalTicketsSoldForEvents(eventIds);
        Long activeAttendances = ticketRepository.getActiveAttendancesForEvents(eventIds);
        Long totalEvents = (long) userEvents.size();
        
        DashboardInsights insights = DashboardInsights.builder()
                .totalTicketsSold(totalTicketsSold != null ? totalTicketsSold : 0L)
                .activeAttendances(activeAttendances != null ? activeAttendances : 0L)
                .totalEvents(totalEvents)
                .totalRevenue(0.0)
                .build();

        return MessageResponse.success("User dashboard insights retrieved", insights);
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setEventId(ticket.getEvent().getId());
        response.setTicketType(ticket.getTicketType());
        response.setPrice(ticket.getPrice());
        response.setQuantityAvailable(ticket.getQuantityAvailable());
        response.setQuantitySold(ticket.getQuantitySold());
        response.setAttendanceStatus(ticket.getAttendanceStatus());
        response.setPurchasedBy(ticket.getPurchasedBy());
        response.setPurchasedAt(ticket.getPurchasedAt());
        response.setScannedAt(ticket.getScannedAt());
        return response;
    }
}