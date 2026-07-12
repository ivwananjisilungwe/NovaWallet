package com.novawallet.novawallet_api.kyc.service;

import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ForbiddenException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.kyc.config.KycConfig;
import com.novawallet.novawallet_api.kyc.dto.ApproveKycRequest;
import com.novawallet.novawallet_api.kyc.dto.KycStatusResponse;
import com.novawallet.novawallet_api.kyc.dto.RejectKycRequest;
import com.novawallet.novawallet_api.kyc.entity.KycDocument;
import com.novawallet.novawallet_api.kyc.enums.KycDocumentStatus;
import com.novawallet.novawallet_api.kyc.enums.KycStatus;
import com.novawallet.novawallet_api.kyc.repository.KycDocumentRepository;
import com.novawallet.novawallet_api.notification.MailService;
import com.novawallet.novawallet_api.notification.entity.NotificationChannel;
import com.novawallet.novawallet_api.notification.entity.NotificationType;
import com.novawallet.novawallet_api.notification.service.NotificationService;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.entity.WalletStatus;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import com.novawallet.novawallet_api.wallet.service.AccountNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminKycService {

    private static final Logger log = LoggerFactory.getLogger(AdminKycService.class);

    private final KycService kycService;
    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final WalletRepository walletRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final KycConfig kycConfig;
    private final FileStorageService fileStorageService;
    private final MailService mailService;
    private final NotificationService notificationService;

    public AdminKycService(
            KycService kycService,
            UserRepository userRepository,
            KycDocumentRepository kycDocumentRepository,
            WalletRepository walletRepository,
            AccountNumberGenerator accountNumberGenerator,
            KycConfig kycConfig,
            FileStorageService fileStorageService,
            MailService mailService,
            NotificationService notificationService
    ) {
        this.kycService = kycService;
        this.userRepository = userRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.walletRepository = walletRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.kycConfig = kycConfig;
        this.fileStorageService = fileStorageService;
        this.mailService = mailService;
        this.notificationService = notificationService;
    }

    // ==================== List Pending Submissions ====================

    @Transactional(readOnly = true)
    public List<User> getPendingSubmissions() {
        return userRepository.findAll().stream()
                .filter(u -> u.getKycStatus() == KycStatus.PENDING)
                .toList();
    }

    // ==================== Get User KYC Detail ====================

    @Transactional(readOnly = true)
    public KycStatusResponse getUserKycDetail(UUID userId) {
        return kycService.getKycStatus(userId);
    }

    // ==================== Get Document File ====================

    @Transactional(readOnly = true)
    public byte[] getDocumentFile(UUID userId, UUID documentId) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (!document.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Document does not belong to this user.");
        }

        return fileStorageService.loadFile(document.getFilePath());
    }

    // ==================== Approve KYC ====================

    public void approveKyc(UUID userId, ApproveKycRequest request, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getKycStatus() != KycStatus.PENDING) {
            throw new BadRequestException("KYC is not in PENDING status. Current status: " + user.getKycStatus());
        }

        // Validate tier exists
        kycConfig.getTier(request.tier());

        user.setKycStatus(KycStatus.APPROVED);
        user.setKycTier(request.tier());
        user.setKycApprovedAt(LocalDateTime.now());
        user.setKycApprovedBy(adminId);
        user.setKycRejectionReason(null);
        user.setKycRejectedAt(null);
        userRepository.save(user);

        // Approve all pending documents
        List<KycDocument> documents = kycDocumentRepository.findByUserIdAndStatus(userId, KycDocumentStatus.PENDING);
        for (KycDocument doc : documents) {
            doc.setStatus(KycDocumentStatus.APPROVED);
            doc.setReviewedAt(LocalDateTime.now());
            doc.setReviewedBy(adminId);
            kycDocumentRepository.save(doc);
        }

        // Auto-create wallet
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .accountNumber(accountNumberGenerator.generate())
                .balance(BigDecimal.ZERO.setScale(2))
                .currency("ZMW")
                .status(WalletStatus.ACTIVE)
                .build();
        walletRepository.save(wallet);

        log.info("KYC approved: userId={}, tier={}, walletAccount={}", userId, request.tier(), wallet.getAccountNumber());

        // Send notification
        mailService.sendKycApprovedEmail(user.getEmail(), user.getFirstName(), request.tier());
        notificationService.recordSent(user.getId(), user.getEmail(), NotificationChannel.EMAIL,
                NotificationType.KYC_APPROVED, "NovaWallet — KYC Approved!",
                "Your KYC has been approved. Tier: " + request.tier());
    }

    // ==================== Reject KYC ====================

    public void rejectKyc(UUID userId, RejectKycRequest request, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getKycStatus() != KycStatus.PENDING) {
            throw new BadRequestException("KYC is not in PENDING status. Current status: " + user.getKycStatus());
        }

        user.setKycStatus(KycStatus.REJECTED);
        user.setKycRejectedAt(LocalDateTime.now());
        user.setKycRejectionReason(request.reason());
        user.setKycApprovedBy(adminId);
        userRepository.save(user);

        // Reject all pending documents
        List<KycDocument> documents = kycDocumentRepository.findByUserIdAndStatus(userId, KycDocumentStatus.PENDING);
        for (KycDocument doc : documents) {
            doc.setStatus(KycDocumentStatus.REJECTED);
            doc.setRejectionReason(request.reason());
            doc.setReviewedAt(LocalDateTime.now());
            doc.setReviewedBy(adminId);
            kycDocumentRepository.save(doc);
        }

        log.info("KYC rejected: userId={}, reason={}", userId, request.reason());
    }
}
