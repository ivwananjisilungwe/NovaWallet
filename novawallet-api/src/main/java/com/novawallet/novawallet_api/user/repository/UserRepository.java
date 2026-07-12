package com.novawallet.novawallet_api.user.repository;

import com.novawallet.novawallet_api.kyc.enums.KycStatus;
import com.novawallet.novawallet_api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") UUID id);

    List<User> findByKycStatusAndKycApprovedAtBefore(KycStatus kycStatus, LocalDateTime cutoff);
}
