package com.example.expense_manager.service;

import com.example.expense_manager.dto.RegisterRequest;
import com.example.expense_manager.dto.ResendOtpRequest;
import com.example.expense_manager.dto.UserResponse;
import com.example.expense_manager.dto.VerifyOtpRequest;
import com.example.expense_manager.entity.User;
import com.example.expense_manager.entity.VerificationOtp;
import com.example.expense_manager.repository.UserRepository;
import com.example.expense_manager.repository.VerificationOtpRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthenticationService {

    private static final int OTP_EXPIRY_MINUTES = 5;

    private final UserRepository userRepository;
    private final VerificationOtpRepository verificationOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthenticationService(
            UserRepository userRepository,
            VerificationOtpRepository verificationOtpRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.verificationOtpRepository = verificationOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(false);

        User saved = userRepository.save(user);

        createAndSendOtp(saved);

        return toUserResponse(saved);
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        VerificationOtp otp = verificationOtpRepository.findByUserEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP không chính xác"));

        if (!otp.getOtpCode().equals(request.otp())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP không chính xác");
        }

        if (otp.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn");
        }

        User user = otp.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        verificationOtpRepository.delete(otp);
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản đã được kích hoạt trước đó");
        }

        createAndSendOtp(user);
    }

    private void createAndSendOtp(User user) {
        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        VerificationOtp otp = verificationOtpRepository.findByUser(user)
                .orElseGet(() -> new VerificationOtp(user, otpCode, expiryTime));

        otp.setOtpCode(otpCode);
        otp.setExpiryTime(expiryTime);
        verificationOtpRepository.save(otp);

        emailService.sendOtpEmail(user.getEmail(), otpCode);
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}
