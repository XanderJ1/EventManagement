package com.bash.Event.ticketing.event.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardInsights {
    private Long totalTicketsSold;
    private Long activeAttendances;
    private Long totalEvents;
    private Double totalRevenue;
}
