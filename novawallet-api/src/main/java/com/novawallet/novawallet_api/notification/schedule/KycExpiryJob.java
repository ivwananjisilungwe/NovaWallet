package com.novawallet.novawallet_api.notification.schedule;

import com.novawallet.novawallet_api.kyc.entity.KycDocument;
import com.novawallet.novawallet_api.kyc.enums.KycDocumentStatus;
import com.novawallet.novawallet_api.kyc.repository.KycDocumentRepository;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class KycExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(KycExpiryJob.class);

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;

    public KycExpiryJob(UserRepository userRepository, KycDocumentRepository kycDocumentRepository) {
        this.userRepository = userRepository;
        this.kycDocumentRepository = kycDocumentRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireKycDocuments() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        List<User> expiredUsers = userRepository.findByKycStatusAndKycApprovedAtBefore(
                com.novawallet.novawallet_api.kyc.enums.KycStatus.APPROVED, oneYearAgo);

        for (User user : expiredUsers) {
            user.setKycStatus(com.novawallet.novawallet_api.kyc.enums.KycStatus.NOT_SUBMITTED);
            user.setKycTier(0);
            userRepository.save(user);

            List<KycDocument> documents = kycDocumentRepository.findByUserId(user.getId());
            for (KycDocument doc : documents) {
                doc.setStatus(KycDocumentStatus.REJECTED);
                kycDocumentRepository.save(doc);
            }

            log.info("KYC expired for user: {} (approved at: {})", user.getId(), user.getKycApprovedAt());
        }

        if (!expiredUsers.isEmpty()) {
            log.info("KYC expiry job: expired {} users", expiredUsers.size());
        }
    }
}
