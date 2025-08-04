package com.bash.Event.ticketing.event.dto.response;


import com.bash.Event.ticketing.event.enums.AttendanceStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketResponse {
    private UUID id;
    private UUID eventId;
    private String ticketType;
    private double price;
    private int quantityAvailable;
    private int quantitySold;
    private AttendanceStatus attendanceStatus;
    private String purchasedBy;
    private LocalDateTime purchasedAt;
    private LocalDateTime scannedAt;
}
