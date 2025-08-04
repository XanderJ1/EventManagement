package com.bash.Event.ticketing.event.service;

import com.bash.Event.ticketing.event.dto.request.PurchaseRequest;
import com.bash.Event.ticketing.event.dto.request.TicketRequest;
import com.bash.Event.ticketing.event.dto.response.DashboardInsights;
import com.bash.Event.ticketing.event.dto.response.MessageResponse;
import com.bash.Event.ticketing.event.dto.response.TicketResponse;

import java.util.List;
import java.util.UUID;

public interface TicketService {
    MessageResponse<TicketResponse> createTicket(UUID eventId, TicketRequest request);
    MessageResponse<List<TicketResponse>> getEventTickets(UUID eventId);
    MessageResponse<TicketResponse> purchaseTicket(UUID eventId, UUID ticketId, PurchaseRequest request);
    MessageResponse<TicketResponse> scanTicket(UUID ticketId);
    MessageResponse<DashboardInsights> getDashboardInsights();
    MessageResponse<DashboardInsights> getUserDashboardInsights();
}