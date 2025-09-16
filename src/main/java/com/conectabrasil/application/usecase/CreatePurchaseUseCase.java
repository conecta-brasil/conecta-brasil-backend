package com.conectabrasil.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.conectabrasil.domain.model.Purchase;
import com.conectabrasil.infrastructure.stellar.SorobanContractService;

@Service
public class CreatePurchaseUseCase {
   
    private final SorobanContractService sorobanService;

    public CreatePurchaseUseCase(
            SorobanContractService sorobanService) {
        this.sorobanService = sorobanService;
    }

    public CreatePurchaseResult execute(String userId, String packageId) {
        try {
            // Gera o XDR não assinado para o cliente assinar
            int packageIdInt = Integer.parseInt(packageId);
            String unsignedXdr = sorobanService.buildBuyAndGrantUnsignedXdr(userId, packageIdInt);

            // Cria a compra com status PENDING (aguardando assinatura)
            Purchase purchase = new Purchase(
                    UUID.randomUUID().toString(),
                    userId,
                    packageId,
                    Instant.now(),
                    null // txHash será preenchido depois da assinatura
            );
        
            return new CreatePurchaseResult(purchase, unsignedXdr);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Package ID deve ser numérico: " + packageId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar compra: " + e.getMessage(), e);
        }
    }

    // Classe interna para retornar tanto a Purchase quanto o XDR
    public static class CreatePurchaseResult {
        private final Purchase purchase;
        private final String unsignedXdr;

        public CreatePurchaseResult(Purchase purchase, String unsignedXdr) {
            this.purchase = purchase;
            this.unsignedXdr = unsignedXdr;
        }

        public Purchase getPurchase() {
            return purchase;
        }

        public String getUnsignedXdr() {
            return unsignedXdr;
        }
    }
}
