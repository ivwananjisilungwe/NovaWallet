/# Phase 1.5 — KYC (Know Your Customer)

> **Status**: Design / Not Started  
> **Depends on**: Phase 1 ✅ (Auth, email, PIN, rate limiting)  
> **Precedes**: Phase 2 (Transactions)  
> **Goal**: Identity verification before wallet activation

---

## 1. What KYC Solves

| Problem | How KYC fixes it |
|---|---|
| Anonymous wallets | User must verify identity before getting a wallet |
| Money laundering | Tracks who owns each wallet |
| Fraud | Face + ID match prevents fake accounts |
| Regulatory compliance | AML checks, transaction limits by tier |
| Wallet limits | Higher KYC tier = higher balance/send limits |

---

## 2. Data Model Changes

### 2.1 New Fields on `User` entity

```sql
-- V5 migration
ALTER TABLE users ADD COLUMN kyc_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED';
ALTER TABLE users ADD COLUMN kyc_tier INTEGER DEFAULT 1;
ALTER TABLE users ADD COLUMN kyc_submitted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN kyc_approved_at TIMESTAMP;
ALTER TABLE users ADD COLUMN kyc_rejected_at TIMESTAMP;
ALTER TABLE users ADD COLUMN kyc_rejection_reason VARCHAR(500);
ALTER TABLE users ADD COLUMN kyc_approved_by UUID;
```

**Java enum:**
```java
public enum KycStatus {
    NOT_SUBMITTED,  // Registered but no KYC
    PENDING,        // Documents submitted, awaiting review
    APPROVED,       // KYC passed, wallet can be created
    REJECTED        // Documents rejected, can resubmit
}
```

### 2.2 New Entity: `KycDocument`

```sql
-- V5 migration (continued)
CREATE TABLE kyc_documents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    document_type VARCHAR(20) NOT NULL,  -- NATIONAL_ID, PASSPORT, SELFIE, PROOF_OF_ADDRESS
    file_path VARCHAR(500) NOT NULL,     -- Path to stored file
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,            -- In bytes
    mime_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    rejection_reason VARCHAR(500),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by UUID,
    version INTEGER DEFAULT 0,

    CONSTRAINT fk_kyc_documents_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_kyc_documents_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE INDEX idx_kyc_documents_user_id ON kyc_documents(user_id);
CREATE INDEX idx_kyc_documents_status ON kyc_documents(status);
```

**Java entity:**
```java
@Entity
@Table(name = "kyc_documents")
public class KycDocument {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private KycDocumentType documentType;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    // ... file_name, file_size, mime_type, status, rejection_reason
    // ... uploaded_at, reviewed_at, reviewed_by
}
```

```java
public enum KycDocumentType {
    NATIONAL_ID,
    PASSPORT,
    SELFIE,
    PROOF_OF_ADDRESS
}

public enum KycDocumentStatus {
    PENDING,
    APPROVED,
    REJECTED
}
```

---

## 3. KYC Tiers

| Tier | Name | Requirements | Wallet Limit | Daily Send Limit |
|---|---|---|---|---|
| 0 | Unverified | None (just registered) | No wallet | None |
| 1 | Basic | Email + phone verified | ZMW 5,000 | ZMW 2,000 |
| 2 | Standard | Tier 1 + National ID + Selfie | ZMW 50,000 | ZMW 20,000 |
| 3 | Advanced | Tier 2 + Proof of Address | ZMW 200,000 | ZMW 100,000 |

**Configurable in `application.yml`:**
```yaml
app:
  kyc:
    tiers:
      tier1:
        name: Basic
        wallet-limit: 5000
        daily-send-limit: 2000
      tier2:
        name: Standard
        wallet-limit: 50000
        daily-send-limit: 20000
        required-documents:
          - NATIONAL_ID
          - SELFIE
      tier3:
        name: Advanced
        wallet-limit: 200000
        daily-send-limit: 100000
        required-documents:
          - NATIONAL_ID
          - SELFIE
          - PROOF_OF_ADDRESS
```

---

## 4. File Storage Strategy

| Environment | Storage | Implementation |
|---|---|---|
| Dev | Local disk | `uploads/kyc/{userId}/{documentId}.jpg` |
| Production | S3-compatible | AWS S3 / DigitalOcean Spaces / MinIO |

**File validation:**
- Max file size: 10MB per document
- Allowed types: `image/jpeg`, `image/png`, `image/webp`, `application/pdf`
- Files scanned for viruses (future: ClamAV integration)
- Original filename preserved with UUID prefix to prevent collisions

---

## 5. Endpoints

### 5.1 User-Facing Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/v1/kyc/documents/upload` | JWT | Upload a KYC document (multipart) |
| `GET` | `/v1/kyc/status` | JWT | Get current KYC status + tier |
| `POST` | `/v1/kyc/submit` | JWT | Submit all uploaded documents for review |

**Upload request** (multipart/form-data):
```
POST /v1/kyc/documents/upload
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data

documentType: NATIONAL_ID
file: [binary file data]
```

**Response:**
```json
{
  "success": true,
  "data": {
    "documentId": "uuid",
    "documentType": "NATIONAL_ID",
    "fileName": "front_id.jpg",
    "fileSize": 245760,
    "status": "PENDING",
    "uploadedAt": "2026-07-10T12:00:00"
  }
}
```

**Submit for review:**
```
POST /v1/kyc/submit
Authorization: Bearer <accessToken>
Content-Type: application/json

{ }
```

**Response:**
```json
{
  "success": true,
  "message": "KYC documents submitted for review",
  "data": {
    "kycStatus": "PENDING",
    "currentTier": 1,
    "submittedAt": "2026-07-10T12:00:00"
  }
}
```

### 5.2 Admin Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/v1/admin/kyc/pending` | ADMIN | List all pending KYC submissions |
| `GET` | `/v1/admin/kyc/{userId}` | ADMIN | View user's KYC documents + status |
| `GET` | `/v1/admin/kyc/{userId}/documents/{documentId}` | ADMIN | Download a specific document |
| `POST` | `/v1/admin/kyc/{userId}/approve` | ADMIN | Approve KYC (with tier assignment) |
| `POST` | `/v1/admin/kyc/{userId}/reject` | ADMIN | Reject KYC (with reason) |

**Approve:**
```
POST /v1/admin/kyc/{userId}/approve
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "tier": 2
}
```

**Triggers:** `kycStatus → APPROVED`, `kycTier → 2`, **wallet auto-created**.

**Reject:**
```
POST /v1/admin/kyc/{userId}/reject
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "reason": "ID document is blurry. Please upload a clearer photo."
}
```

**Triggers:** `kycStatus → REJECTED`, `kycRejectionReason → "..."`, documents can be re-uploaded.

---

## 6. Wallet Auto-Creation Logic

When admin approves KYC:

```java
@Transactional
public void approveKyc(UUID userId, int tier) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found"));
    
    // 1. Update user status
    user.setKycStatus(KycStatus.APPROVED);
    user.setKycTier(tier);
    user.setKycApprovedAt(LocalDateTime.now());
    user.setKycApprovedBy(adminId);
    userRepository.save(user);
    
    // 2. Create wallet automatically
    Wallet wallet = Wallet.builder()
        .userId(user.getId())
        .accountNumber(accountNumberGenerator.generate())
        .balance(BigDecimal.ZERO)
        .currency("ZMW")
        .status(WalletStatus.ACTIVE)
        .build();
    walletRepository.save(wallet);
    
    // 3. Send notification (async)
    mailService.sendKycApprovedEmail(user.getEmail(), user.getFirstName(), tier);
}
```

---

## 7. Wallet Gating (for Phase 2)

Every transaction endpoint checks KYC before allowing operations:

```java
private void verifyKyc(User user) {
    if (user.getKycStatus() != KycStatus.APPROVED) {
        throw new ForbiddenException(
            "KYC verification required. Please complete identity verification first."
        );
    }
}

private void checkWalletLimit(User user, Wallet wallet, BigDecimal amount) {
    KycTier tier = kycTierConfig.getTier(user.getKycTier());
    if (wallet.getBalance().compareTo(tier.getWalletLimit()) > 0) {
        throw new BadRequestException(
            "Wallet balance exceeds your KYC tier limit of ZMW " + tier.getWalletLimit()
        );
    }
}
```

---

## 8. Security Considerations

| Concern | Mitigation |
|---|---|
| Document storage | Files stored outside the application root, served only through admin endpoints |
| ID forgery | Admin manually reviews documents (Phase 1.5) → automated verification (future) |
| Re-upload abuse | Rate-limit uploads (5 per hour) |
| Admin access | All admin endpoints require `ADMIN` role |
| Audit trail | Every KYC action (submit, approve, reject) logged with timestamp + admin ID |
| Data retention | KYC documents stored for 5 years (regulatory), then purged |
| Encryption | Files encrypted at rest in production |

---

## 9. Implementation Order

| Step | Task | Files | Effort |
|---|---|---|---|
| 1 | V5 migration — add kyc columns to users + create kyc_documents table | `V5__add_kyc_tables.sql` | Small |
| 2 | Update User entity — add kycStatus, kycTier, etc. | `User.java` | Small |
| 3 | Create KycDocument entity, enums (KycStatus, KycDocumentType, KycDocumentStatus) | `kyc/entity/*`, `kyc/enums/*` | Small |
| 4 | Create KycDocumentRepository | `kyc/repository/KycDocumentRepository.java` | Small |
| 5 | Create KycService — upload, submit, approve, reject, wallet creation | `kyc/service/KycService.java` | Medium |
| 6 | File storage service (local dev, S3 abstraction) | `kyc/service/FileStorageService.java` | Medium |
| 7 | KYC DTOs — KycDocumentResponse, KycStatusResponse, ApproveRequest, RejectRequest | `kyc/dto/*` | Small |
| 8 | KYC config — tiers, limits, allowed file types | `application.yml` + `KycConfig.java` | Small |
| 9 | KycController — upload, status, submit | `kyc/controller/KycController.java` | Medium |
| 10 | AdminKycController — list pending, view, approve, reject, download | `admin/controller/AdminKycController.java` | Medium |
| 11 | Exceptions — KycAlreadySubmittedException, KycDocumentRejectedException | `exception/*` | Small |
| 12 | Wallet auto-creation on approval | `KycService.approveKyc()` | Small |
| 13 | Update SecurityConfig — permit new public paths, lock down admin paths | `SecurityConfig.java` | Small |
| 14 | Tests — KycService unit tests | `kyc/service/KycServiceTest.java` | Medium |
| 15 | Tests — KycController integration tests | `kyc/controller/KycControllerIntegrationTest.java` | Medium |
| 16 | Tests — AdminKycController integration tests | `admin/controller/AdminKycControllerIntegrationTest.java` | Medium |
| 17 | Update SRS | `SRS.md` | Small |
| 18 | Update project plan | `novawallet-project-plan.md` | Small |

---

## 10. Estimated Timeline

| Scope | Time |
|---|---|
| Basic KYC (upload + submit + admin approve/reject + wallet creation) | ~2-3 days |
| File storage service | ~0.5 day |
| Tests | ~1 day |
| **Total** | **~3-4 days** |

---

## 11. Open Questions (to decide)

- [ ] **Automated verification?** Do we integrate a 3rd-party KYC service (Smile Identity, YouVerify, Dojah) or start with manual admin review?
- [ ] **Selfie verification?** Do we need face match (compare selfie to ID photo) in Phase 1.5, or just collect documents?
- [ ] **File storage?** Local disk for dev is fine. For production, which service? (S3, DigitalOcean Spaces, Cloudinary?)
- [ ] **Webhook for approval?** Should there be a webhook/callback when KYC is approved so the frontend can react?
- [ ] **Re-submission?** How many times can a user re-submit after rejection? (Unlimited? Max 3?)
