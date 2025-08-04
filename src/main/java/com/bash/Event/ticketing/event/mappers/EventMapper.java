package com.bash.Event.ticketing.event.mappers;

import com.bash.Event.ticketing.event.dto.request.EventRequest;
import com.bash.Event.ticketing.event.dto.response.EventResponse;
import com.bash.Event.ticketing.event.model.Address;
import com.bash.Event.ticketing.event.model.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public Event mapToEvent(EventRequest eventRequest) {

        if (eventRequest == null) {
            return null;
        }

        Address address = Address.builder()
                .venueName(eventRequest.getVenueName())
                .streetAddress(eventRequest.getStreetAddress())
                .city(eventRequest.getCity())
                .country(eventRequest.getCountry())
                .build();

        return Event.builder()
                .title(eventRequest.getEventName())
                .description(eventRequest.getDescription())
                .venue(address)
                .startTime(eventRequest.getStartTime())
                .endTime(eventRequest.getEndTime())
                .build();
    }

    public EventResponse mapToEventResponse(Event event) {

        if (event == null) {
            return null;
        }

        return EventResponse.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .address(event.getVenue())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .build();
    }
}
