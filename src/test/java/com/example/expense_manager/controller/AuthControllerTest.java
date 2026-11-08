package com.example.expense_manager.controller;

import com.example.expense_manager.dto.LoginRequest;
import com.example.expense_manager.dto.RegisterRequest;
import com.example.expense_manager.dto.ResendOtpRequest;
import com.example.expense_manager.dto.VerifyOtpRequest;
import com.example.expense_manager.repository.UserRepository;
import com.example.expense_manager.repository.VerificationOtpRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationOtpRepository verificationOtpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersValidUser() throws Exception {
        RegisterRequest request = new RegisterRequest("alice@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void rejectsDuplicateEmailOnRegister() throws Exception {
        RegisterRequest request = new RegisterRequest("bob@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void storesPasswordAsBcryptHashNotPlaintext() throws Exception {
        RegisterRequest request = new RegisterRequest("carol@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String storedHash = userRepository.findByEmail("carol@example.com").orElseThrow().getPasswordHash();

        assertThat(storedHash).isNotEqualTo("password123");
        assertThat(storedHash).startsWith("$2");
        assertThat(passwordEncoder.matches("password123", storedHash)).isTrue();
    }

    @Test
    void logsInWithCorrectCredentials() throws Exception {
        registerUser("dave@example.com", "password123");

        LoginRequest login = new LoginRequest("dave@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dave@example.com"));
    }

    @Test
    void rejectsLoginWithWrongPassword() throws Exception {
        registerUser("erin@example.com", "password123");

        LoginRequest login = new LoginRequest("erin@example.com", "wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLoginWithUnknownEmail() throws Exception {
        LoginRequest login = new LoginRequest("ghost@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsPostWithoutCsrfToken() throws Exception {
        RegisterRequest request = new RegisterRequest("ivan@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByEmail("ivan@example.com")).isFalse();
    }

    @Test
    @org.springframework.test.annotation.DirtiesContext
    void rejectsPostWithInvalidCsrfToken() throws Exception {
        RegisterRequest request = new RegisterRequest("judy@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf().useInvalidToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByEmail("judy@example.com")).isFalse();
    }

    @Test
    void csrfEndpointIsPublicAndWritesXsrfTokenCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void rejectsAccessToCurrentUserWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOwnIdentityAfterLogin() throws Exception {
        registerUser("frank@example.com", "password123");
        registerUser("grace@example.com", "password123");

        MockHttpSession session = loginAndGetSession("frank@example.com", "password123");

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("frank@example.com"));
    }

    @Test
    void logoutInvalidatesSessionSoCurrentUserIsNoLongerAccessible() throws Exception {
        registerUser("heidi@example.com", "password123");
        MockHttpSession session = loginAndGetSession("heidi@example.com", "password123");

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLoginWhenUserNotVerified() throws Exception {
        RegisterRequest request = new RegisterRequest("unverified@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("unverified@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyOtpRejectsWrongCode() throws Exception {
        RegisterRequest request = new RegisterRequest("wrong-otp@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest("wrong-otp@example.com", "000000"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendOtpGeneratesNewOtp() throws Exception {
        RegisterRequest request = new RegisterRequest("resend@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String firstOtp = verificationOtpRepository.findByUserEmail("resend@example.com").orElseThrow().getOtpCode();

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ResendOtpRequest("resend@example.com"))))
                .andExpect(status().isOk());

        assertThat(verificationOtpRepository.findByUserEmail("resend@example.com")).isPresent();
    }

    private void registerUser(String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password);
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String otp = verificationOtpRepository.findByUserEmail(email).orElseThrow().getOtpCode();
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, otp))))
                .andExpect(status().isOk());
    }

    private MockHttpSession loginAndGetSession(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
