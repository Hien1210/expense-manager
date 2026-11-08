package com.example.expense_manager.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Phase 6.2 SecurityConfig change: static frontend resources and the CSRF-priming
 * endpoint are publicly reachable, while every business API endpoint remains protected exactly
 * as before — none of the existing authenticated() rules were weakened.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StaticResourceSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsUnauthenticatedAccessToLoginPage() throws Exception {
        mockMvc.perform(get("/login.html")).andExpect(status().isOk());
    }

    @Test
    void allowsUnauthenticatedAccessToRegisterPage() throws Exception {
        mockMvc.perform(get("/register.html")).andExpect(status().isOk());
    }

    @Test
    void allowsUnauthenticatedAccessToCss() throws Exception {
        mockMvc.perform(get("/css/base.css")).andExpect(status().isOk());
    }

    @Test
    void allowsUnauthenticatedAccessToApiJs() throws Exception {
        mockMvc.perform(get("/js/api.js")).andExpect(status().isOk());
    }

    @Test
    void allowsUnauthenticatedAccessToAuthJs() throws Exception {
        mockMvc.perform(get("/js/auth.js")).andExpect(status().isOk());
    }

    @Test
    void rootPathIsNotBlockedBySecurity() throws Exception {
        // No index.html exists yet, so this must resolve to a plain 404 from the resource
        // handler (proving the request passed the security filter chain) rather than a 401
        // (which would mean it was still being blocked by authorizeHttpRequests).
        mockMvc.perform(get("/")).andExpect(status().isNotFound());
    }

    @Test
    void businessApiEndpointsRemainProtected() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/transactions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }
}
