package com.bash.Event.ticketing.event.model;

import com.bash.Event.ticketing.event.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String ticketType;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private int quantityAvailable;

    @Column(nullable = false)
    private int quantitySold = 0;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus attendanceStatus = AttendanceStatus.NOT_ATTENDED;

    @Column(name = "purchased_by")
    private String purchasedBy; // Email or identifier

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;
}
