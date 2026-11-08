package com.example.expense_manager.repository;

import com.example.expense_manager.entity.User;
import com.example.expense_manager.entity.VerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationOtpRepository extends JpaRepository<VerificationOtp, Long> {

    Optional<VerificationOtp> findByUser(User user);

    Optional<VerificationOtp> findByUserEmail(String email);

    void deleteByUser(User user);
}

