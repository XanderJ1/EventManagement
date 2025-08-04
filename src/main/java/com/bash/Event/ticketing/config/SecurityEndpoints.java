package com.bash.Event.ticketing.config;

public final class SecurityEndpoints {
    
    private SecurityEndpoints() {
        // Utility class - prevent instantiation
    }
    
    // Public endpoints - no authentication required
    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/**",
        "/api/v1/sse/**",
        "/v3/api-docs/**", 
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/", 
        "/index.html", 
        "/admin.html", 
        "/home.html",
        "/static/**", 
        "/css/**", 
        "/js/**", 
        "/images/**",
        "/favicon.ico", 
        "/error", 
        "/webjars/**",
        "/h2-console/**"
    };
    
    // Event viewing endpoints - public access (more specific patterns)
    public static final String[] PUBLIC_EVENT_ENDPOINTS = {
        "/api/v1/events",
        "/api/v1/events/*/tickets"
    };

    // Individual event viewing by UUID - public access
    public static final String[] PUBLIC_EVENT_DETAIL_ENDPOINTS = {
        "/api/v1/events/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    };

    // Ticket purchase endpoints - public access
    public static final String[] PUBLIC_TICKET_ENDPOINTS = {
        "/api/v1/events/*/tickets/*/purchase"
    };
    
    // Event management endpoints - authenticated users only
    public static final String[] AUTHENTICATED_EVENT_ENDPOINTS = {
        "/api/v1/events",
        "/api/v1/events/*"
    };
    
    // Ticket management endpoints - authenticated users only
    public static final String[] AUTHENTICATED_TICKET_ENDPOINTS = {
        "/api/v1/events/*/tickets",
        "/api/v1/tickets/*/scan"
    };
    
    // Authenticated dashboard endpoints - must come BEFORE wildcard patterns
    public static final String[] AUTHENTICATED_DASHBOARD_ENDPOINTS = {
        "/api/v1/events/dashboard/**",
        "/api/v1/events/my-events"
    };
    
    // Admin endpoints - admin role required
    public static final String[] ADMIN_ENDPOINTS = {
        "/api/admin"
    };
    
    // User endpoints - user or admin role required
    public static final String[] USER_ENDPOINTS = {
        "/api/user"
    };
}