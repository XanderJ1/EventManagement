package com.bash.Event.ticketing.event.repository;

import com.bash.Event.ticketing.event.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    boolean existsByTitleIgnoreCaseAndStartTime (String title, LocalDateTime startTime);
    Page<Event> findByCreatedBy(String createdBy, Pageable pageable);
    List<Event> findByCreatedBy(String createdBy);

}
