package com.bash.Event.ticketing.authentication.controller;




import com.bash.Event.ticketing.authentication.dto.response.MessageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ResourceController {

    @GetMapping("/public")
    public MessageResponse publicAccess() {
        return new MessageResponse("Public content.");
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public MessageResponse userAccess() {
        return new MessageResponse("User content.");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public MessageResponse adminAccess() {
        return new MessageResponse("Admin Board.");
    }
}