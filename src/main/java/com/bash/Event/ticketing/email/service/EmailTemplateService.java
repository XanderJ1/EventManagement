package com.bash.Event.ticketing.email.service;

public interface EmailTemplateService {
    /**
     * Generates the content for verification email
     * @param name user's name
     * @param verificationLink the verification link
     * @param expirationTime token expiration time
     * @return formatted HTML content
     */
    String getVerificationEmailContent(String name, String verificationLink, String expirationTime);
    
    /**
     * Generates the content for password reset email
     * @param name user's name
     * @param resetLink the password reset link
     * @param expirationTime token expiration time
     * @return formatted HTML content
     */
    String getPasswordResetEmailContent(String name, String resetLink, String expirationTime);
}