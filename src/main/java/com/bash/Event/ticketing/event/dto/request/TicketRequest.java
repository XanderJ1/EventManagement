package com.bash.Event.ticketing.event.dto.request;

import lombok.Data;

@Data
public class TicketRequest {
    private String ticketType;
    private double price;
    private int quantityAvailable;
}
