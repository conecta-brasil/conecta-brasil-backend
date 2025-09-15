package com.conectabrasil.adapter.inboud.rest.dto;


import java.time.Instant;

import com.conectabrasil.domain.model.Purchase;


public record PurchaseResponse(
        String id,
        String userId,
        String packageId,
        Instant createdAt,
        String txHash,
        String unsignedXdr // Novo campo para o XDR
) {
    public static PurchaseResponse from(Purchase purchase) {
        return new PurchaseResponse(
                purchase.getId(),
                purchase.getUserId(),
                purchase.getPackageId(),
                purchase.getCreatedAt(),
                purchase.getStellarTx(),
                null // XDR será null para purchases existentes
        );
    }

    public static PurchaseResponse from(Purchase purchase, String unsignedXdr) {
        return new PurchaseResponse(
                purchase.getId(),
                purchase.getUserId(),
                purchase.getPackageId(),
                purchase.getCreatedAt(),
                purchase.getStellarTx(),
                unsignedXdr);
    }
}
