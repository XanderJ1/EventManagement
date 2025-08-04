package com.bash.Event.ticketing.event.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest {

    private String eventName;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String venueName;

    private String streetAddress;

    private String city;

    private String country;
}
