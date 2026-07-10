package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TransactionSpecifications {

    private TransactionSpecifications() {}

    public static Specification<Transaction> withFilters(
            UUID walletId,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Wallet involvement: sender OR receiver
            predicates.add(cb.or(
                    cb.equal(root.get("senderWalletId"), walletId),
                    cb.equal(root.get("receiverWalletId"), walletId)
            ));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Transaction> byWalletId(UUID walletId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("senderWalletId"), walletId),
                cb.equal(root.get("receiverWalletId"), walletId)
        );
    }
}
