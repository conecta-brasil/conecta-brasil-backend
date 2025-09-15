package com.conectabrasil.domain.model;

import java.time.Instant;

import lombok.Data;

@Data
public class Purchase {
    private final String id;
    private final String userId;
    private final String packageId;
    private final Instant createdAt;
    private final String stellarTx;
}
