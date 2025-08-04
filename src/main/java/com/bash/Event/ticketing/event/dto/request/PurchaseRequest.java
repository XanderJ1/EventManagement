package com.bash.Event.ticketing.event.dto.request;

import lombok.Data;

@Data
public class PurchaseRequest {
    private String purchaserEmail;
    private int quantity = 1;
}
