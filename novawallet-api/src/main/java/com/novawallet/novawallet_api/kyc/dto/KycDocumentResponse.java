package com.novawallet.novawallet_api.kyc.dto;

import com.novawallet.novawallet_api.kyc.entity.KycDocument;
import com.novawallet.novawallet_api.kyc.enums.KycDocumentStatus;
import com.novawallet.novawallet_api.kyc.enums.KycDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "KYC document response")
public record KycDocumentResponse(
        @Schema(description = "Document UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Document type", example = "NATIONAL_ID")
        KycDocumentType documentType,

        @Schema(description = "Original filename", example = "front_id.jpg")
        String fileName,

        @Schema(description = "File size in bytes", example = "245760")
        long fileSize,

        @Schema(description = "MIME type", example = "image/jpeg")
        String mimeType,

        @Schema(description = "Document review status", example = "PENDING")
        KycDocumentStatus status,

        @Schema(description = "Rejection reason (if rejected)")
        String rejectionReason,

        @Schema(description = "Upload timestamp")
        LocalDateTime uploadedAt
) {
    public static KycDocumentResponse from(KycDocument doc) {
        return new KycDocumentResponse(
                doc.getId(),
                doc.getDocumentType(),
                doc.getFileName(),
                doc.getFileSize(),
                doc.getMimeType(),
                doc.getStatus(),
                doc.getRejectionReason(),
                doc.getUploadedAt()
        );
    }
}
