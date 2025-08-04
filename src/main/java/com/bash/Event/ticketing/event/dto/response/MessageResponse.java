package com.bash.Event.ticketing.event.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse<T> {

    private String status;
    private String code;
    private String message;
    private T data;

    public MessageResponse(String message) {
        this.message = message;

    }

    public static <T> MessageResponse<T> success(String message) {
        return MessageResponse.<T>builder()
                .status("success")
                .code("200")
                .message(message)
                .build();
    }

    public static <T> MessageResponse <T> success(String message, T data) {
        return MessageResponse.<T>builder()
                .status("success")
                .code("200")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> MessageResponse<T> error(String message, HttpStatusCode status) {
        return MessageResponse.<T>builder()
                .status("error")
                .code(String.valueOf(status))
                .message(message)
                .build();
    }

}
