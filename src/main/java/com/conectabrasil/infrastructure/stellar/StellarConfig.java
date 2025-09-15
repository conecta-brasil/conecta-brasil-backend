package com.conectabrasil.infrastructure.stellar;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "stellar")
public class StellarConfig {
    private String networkPassphrase;
    private String horizonUrl;
    private String sorobanRpcUrl;
    private String contractAddress;
    private String adminSecretKey;
    private String tokenAssetAddress;
}
