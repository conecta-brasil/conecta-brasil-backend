package com.conectabrasil.domain.model;

import java.time.Instant;

import lombok.Data;

@Data
public class Grant {
    private final String id;
    private final String callerUserId;
    private final String ownerUserId;
    private final String orderId;
    private final Instant createdAt;
    private final String stellarTx;
}