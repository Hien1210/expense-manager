package com.example.expense_manager.controller;

import com.example.expense_manager.dto.LoginRequest;
import com.example.expense_manager.dto.RegisterRequest;
import com.example.expense_manager.dto.ResendOtpRequest;
import com.example.expense_manager.dto.UserResponse;
import com.example.expense_manager.dto.VerifyOtpRequest;
import com.example.expense_manager.security.UserPrincipal;
import com.example.expense_manager.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final org.springframework.security.web.csrf.CookieCsrfTokenRepository cookieCsrfTokenRepository =
            org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse();

    public AuthController(AuthenticationService authenticationService, AuthenticationManager authenticationManager) {
        this.authenticationService = authenticationService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    @PostMapping("/verify-otp")
    @ResponseStatus(HttpStatus.OK)
    public void verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authenticationService.verifyOtp(request);
    }

    @PostMapping("/resend-otp")
    @ResponseStatus(HttpStatus.OK)
    public void resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authenticationService.resendOtp(request);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return authenticationService.toUserResponse(principal.getUser());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authenticationService.toUserResponse(principal.getUser());
    }

    /**
     * Forces the deferred CSRF token to be resolved, which makes Spring Security write the
     * XSRF-TOKEN cookie on the response. CSRF protection only validates (and therefore only
     * resolves/persists the token) on state-changing HTTP methods, so a plain page load never
     * triggers it on its own — the frontend must call this once before submitting any form.
     */
    @GetMapping("/csrf")
    public void csrf(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CsrfToken token = (CsrfToken) httpRequest.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = (CsrfToken) httpRequest.getAttribute("_csrf");
        }
        if (token != null) {
            token.getToken();
            cookieCsrfTokenRepository.saveToken(token, httpRequest, httpResponse);
        }
    }
}
