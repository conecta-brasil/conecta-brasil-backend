package com.conectabrasil.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.conectabrasil.domain.model.Grant;
import com.conectabrasil.infrastructure.stellar.SorobanContractService;

@Service
public class CreateGrantUseCase {
   
    private final SorobanContractService sorobanService;

    public CreateGrantUseCase(
            SorobanContractService sorobanService) {
        this.sorobanService = sorobanService;
    }

    public CreateGrantResult execute(String callerUserId, String ownerUserId, String orderId) {
        try {
            // Gera o XDR não assinado para o cliente assinar
            int orderIdInt = Integer.parseInt(orderId);
            String unsignedXdr = sorobanService.buildGrantUnsignedXdrForTest(callerUserId, ownerUserId, orderIdInt);

            // Cria o grant com status PENDING (aguardando assinatura)
            Grant grant = new Grant(
                    UUID.randomUUID().toString(),
                    callerUserId,
                    ownerUserId,
                    orderId,
                    Instant.now(),
                    null // txHash será preenchido depois da assinatura
            );
        
            return new CreateGrantResult(grant, unsignedXdr);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Order ID deve ser numérico: " + orderId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar grant: " + e.getMessage(), e);
        }
    }

    // Classe interna para retornar tanto o Grant quanto o XDR
    public static class CreateGrantResult {
        private final Grant grant;
        private final String unsignedXdr;

        public CreateGrantResult(Grant grant, String unsignedXdr) {
            this.grant = grant;
            this.unsignedXdr = unsignedXdr;
        }

        public Grant getGrant() {
            return grant;
        }

        public String getUnsignedXdr() {
            return unsignedXdr;
        }
    }
}