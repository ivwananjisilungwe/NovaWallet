package com.novawallet.novawallet_api.kyc.repository;

import com.novawallet.novawallet_api.kyc.entity.KycDocument;
import com.novawallet.novawallet_api.kyc.enums.KycDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByUserId(UUID userId);

    List<KycDocument> findByUserIdAndStatus(UUID userId, KycDocumentStatus status);

    long countByUserIdAndStatus(UUID userId, KycDocumentStatus status);
}
