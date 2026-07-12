package com.novawallet.novawallet_api.wallet.service;

import com.novawallet.novawallet_api.audit.service.AuditService;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.wallet.dto.WalletResponse;
import com.novawallet.novawallet_api.wallet.entity.FreezeReason;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.entity.WalletStatus;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final AuditService auditService;

    public WalletService(WalletRepository walletRepository, AuditService auditService) {
        this.walletRepository = walletRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByAccountNumber(String accountNumber) {
        Wallet wallet = walletRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + accountNumber));
        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWalletEntity(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletId));
    }

    public WalletResponse freezeWallet(UUID walletId, FreezeReason reason, UUID adminId) {
        Wallet wallet = getWalletEntity(walletId);
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new BadRequestException("Wallet is already frozen");
        }
        wallet.setStatus(WalletStatus.FROZEN);
        wallet.setFreezeReason(reason);
        wallet = walletRepository.save(wallet);
        log.info("Wallet {} frozen by admin {}. Reason: {}", walletId, adminId, reason);
        auditService.recordAction("Wallet", walletId, "WALLET_FREEZE",
                "ACTIVE", "FROZEN", adminId);
        return WalletResponse.from(wallet);
    }

    public WalletResponse unfreezeWallet(UUID walletId, UUID adminId) {
        Wallet wallet = getWalletEntity(walletId);
        if (wallet.getStatus() != WalletStatus.FROZEN) {
            throw new BadRequestException("Wallet is not frozen");
        }
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setFreezeReason(null);
        wallet = walletRepository.save(wallet);
        log.info("Wallet {} unfrozen by admin {}", walletId, adminId);
        auditService.recordAction("Wallet", walletId, "WALLET_UNFREEZE",
                "FROZEN", "ACTIVE", adminId);
        return WalletResponse.from(wallet);
    }
}
