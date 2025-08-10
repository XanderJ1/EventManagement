package com.bash.Event.ticketing.Exceptions;

import java.time.LocalDateTime;

public record ErrorResponse(int status, String message, LocalDateTime time) {
    public ErrorResponse(int status, String message){
        this(status, message, LocalDateTime.now());
    }
}
