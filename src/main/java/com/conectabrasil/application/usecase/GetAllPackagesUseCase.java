package com.conectabrasil.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.conectabrasil.infrastructure.stellar.SorobanContractService;

@Service
public class GetAllPackagesUseCase {

    private final SorobanContractService sorobanService;

    public GetAllPackagesUseCase(SorobanContractService sorobanService) {
        this.sorobanService = sorobanService;
    }

    /**
     * Executa a busca de todos os pacotes disponíveis no contrato Stellar
     * 
     * @return Resultado da operação com lista de pacotes
     */
    public GetAllPackagesResult execute() {
        try {
            List<Object> packages = sorobanService.getAllPackages();
            return new GetAllPackagesResult(packages, true, null);
        } catch (Exception e) {
            return new GetAllPackagesResult(null, false, "Erro ao buscar pacotes: " + e.getMessage());
        }
    }

    /**
     * Resultado da operação de busca de pacotes
     */
    public static class GetAllPackagesResult {
        private final List<Object> packages;
        private final boolean success;
        private final String errorMessage;

        public GetAllPackagesResult(List<Object> packages, boolean success, String errorMessage) {
            this.packages = packages;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public List<Object> getPackages() {
            return packages;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}