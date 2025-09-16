package com.conectabrasil.adapter.inboud.rest.dto;

import java.time.Instant;

import com.conectabrasil.domain.model.Grant;

public record GrantResponse(
        String id,
        String callerUserId,
        String ownerUserId,
        String orderId,
        Instant createdAt,
        String txHash,
        String unsignedXdr // Campo para o XDR não assinado
) {
    public static GrantResponse from(Grant grant) {
        return new GrantResponse(
                grant.getId(),
                grant.getCallerUserId(),
                grant.getOwnerUserId(),
                grant.getOrderId(),
                grant.getCreatedAt(),
                grant.getStellarTx(),
                null // XDR será null para grants existentes
        );
    }

    public static GrantResponse from(Grant grant, String unsignedXdr) {
        return new GrantResponse(
                grant.getId(),
                grant.getCallerUserId(),
                grant.getOwnerUserId(),
                grant.getOrderId(),
                grant.getCreatedAt(),
                grant.getStellarTx(),
                unsignedXdr);
    }
}