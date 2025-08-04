package com.bash.Event.ticketing.event.repository;

import com.bash.Event.ticketing.event.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    
    List<Ticket> findByEventId(UUID eventId);
    
    @Query("SELECT SUM(t.quantitySold) FROM Ticket t")
    Long getTotalTicketsSold();
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.attendanceStatus = 'ATTENDED'")
    Long getActiveAttendances();

    @Query("SELECT SUM(t.quantitySold) FROM Ticket t WHERE t.event.id IN :eventIds")
    Long getTotalTicketsSoldForEvents(@Param("eventIds") List<UUID> eventIds);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.attendanceStatus = 'ATTENDED' AND t.event.id IN :eventIds")
    Long getActiveAttendancesForEvents(@Param("eventIds") List<UUID> eventIds);
}
