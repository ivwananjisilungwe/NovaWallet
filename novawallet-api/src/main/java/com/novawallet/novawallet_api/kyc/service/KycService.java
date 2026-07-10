package com.novawallet.novawallet_api.kyc.service;

import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.kyc.config.KycConfig;
import com.novawallet.novawallet_api.kyc.dto.KycDocumentResponse;
import com.novawallet.novawallet_api.kyc.dto.KycStatusResponse;
import com.novawallet.novawallet_api.kyc.entity.KycDocument;
import com.novawallet.novawallet_api.kyc.enums.KycDocumentStatus;
import com.novawallet.novawallet_api.kyc.enums.KycDocumentType;
import com.novawallet.novawallet_api.kyc.enums.KycStatus;
import com.novawallet.novawallet_api.kyc.repository.KycDocumentRepository;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class KycService {

    private static final Logger log = LoggerFactory.getLogger(KycService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );
    private static final int MAX_UPLOADS_PER_HOUR = 5;

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final FileStorageService fileStorageService;
    private final KycConfig kycConfig;

    public KycService(
            UserRepository userRepository,
            KycDocumentRepository kycDocumentRepository,
            FileStorageService fileStorageService,
            KycConfig kycConfig
    ) {
        this.userRepository = userRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.fileStorageService = fileStorageService;
        this.kycConfig = kycConfig;
    }

    // ==================== Upload Document ====================

    public KycDocumentResponse uploadDocument(UUID userId, KycDocumentType documentType, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getKycStatus() == KycStatus.APPROVED) {
            throw new BadRequestException("KYC is already approved. Cannot upload more documents.");
        }

        if (user.getKycStatus() == KycStatus.PENDING) {
            throw new BadRequestException("KYC is pending review. Cannot upload more documents until reviewed.");
        }

        validateFile(file);

        checkUploadRateLimit(userId);

        // Save entity first to get auto-generated ID, with placeholder filePath
        KycDocument document = KycDocument.builder()
                .user(user)
                .documentType(documentType)
                .filePath("pending")
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .status(KycDocumentStatus.PENDING)
                .build();

        document = kycDocumentRepository.saveAndFlush(document);

        // Store file using the generated ID, then update the filePath
        String filePath = fileStorageService.storeFile(file, userId, document.getId());
        document.setFilePath(filePath);
        document = kycDocumentRepository.save(document);

        log.info("KYC document uploaded: user={}, type={}, docId={}", userId, documentType, document.getId());

        return KycDocumentResponse.from(document);
    }

    // ==================== Get KYC Status ====================

    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<KycDocument> documents = kycDocumentRepository.findByUserId(userId);
        List<KycDocumentResponse> docResponses = documents.stream()
                .map(KycDocumentResponse::from)
                .toList();

        KycConfig.TierConfig tierConfig = null;
        String tierName = "Unverified";
        String walletLimit = "0";
        String dailySendLimit = "0";

        if (user.getKycTier() > 0) {
            try {
                tierConfig = kycConfig.getTier(user.getKycTier());
                tierName = tierConfig.getName();
                walletLimit = tierConfig.getWalletLimit().toString();
                dailySendLimit = tierConfig.getDailySendLimit().toString();
            } catch (IllegalArgumentException e) {
                log.warn("Unknown KYC tier {} for user {}", user.getKycTier(), userId);
            }
        }

        return new KycStatusResponse(
                user.getKycStatus(),
                user.getKycTier(),
                tierName,
                walletLimit,
                dailySendLimit,
                docResponses,
                user.getKycSubmittedAt(),
                user.getKycApprovedAt(),
                user.getKycRejectionReason()
        );
    }

    // ==================== Submit for Review ====================

    public KycStatusResponse submitForReview(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getKycStatus() == KycStatus.PENDING) {
            throw new BadRequestException("KYC is already pending review.");
        }

        if (user.getKycStatus() == KycStatus.APPROVED) {
            throw new BadRequestException("KYC is already approved.");
        }

        List<KycDocument> documents = kycDocumentRepository.findByUserId(userId);

        if (documents.isEmpty()) {
            throw new BadRequestException("Please upload at least one KYC document before submitting.");
        }

        if (documents.stream().anyMatch(d -> d.getStatus() == KycDocumentStatus.PENDING && !isAcceptablePending(d))) {
            throw new BadRequestException("Some documents have been rejected. Please re-upload them before submitting.");
        }

        user.setKycStatus(KycStatus.PENDING);
        user.setKycSubmittedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("KYC submitted for review: userId={}", userId);

        return getKycStatus(userId);
    }

    // ==================== Private Helpers ====================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds the maximum limit of 10MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid file type. Allowed types: " + String.join(", ", ALLOWED_MIME_TYPES)
            );
        }
    }

    private void checkUploadRateLimit(UUID userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<KycDocument> recentUploads = kycDocumentRepository.findByUserId(userId).stream()
                .filter(d -> d.getUploadedAt().isAfter(oneHourAgo))
                .toList();

        if (recentUploads.size() >= MAX_UPLOADS_PER_HOUR) {
            throw new BadRequestException("Upload limit reached. Maximum " + MAX_UPLOADS_PER_HOUR + " uploads per hour.");
        }
    }

    private boolean isAcceptablePending(KycDocument document) {
        return document.getStatus() == KycDocumentStatus.PENDING
                || document.getStatus() == KycDocumentStatus.APPROVED;
    }
}
