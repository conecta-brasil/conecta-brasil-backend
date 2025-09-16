package com.conectabrasil.adapter.inboud.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.conectabrasil.application.usecase.GetAllPackagesUseCase;
import com.conectabrasil.application.usecase.GetAllPackagesUseCase.GetAllPackagesResult;
import com.conectabrasil.infrastructure.stellar.SorobanContractService;

import java.util.List;

@RestController
@RequestMapping("/packages")
public class PackageController {

    private final GetAllPackagesUseCase getAllPackagesUseCase;
    private final SorobanContractService sorobanContractService;

    public PackageController(GetAllPackagesUseCase getAllPackagesUseCase,
            SorobanContractService sorobanContractService) {
        this.getAllPackagesUseCase = getAllPackagesUseCase;
        this.sorobanContractService = sorobanContractService;
    }

    /**
     * Endpoint para buscar todos os pacotes disponíveis
     * Invoca a função get_all_packages do contrato Stellar
     * 
     * @return Lista de pacotes disponíveis ou erro
     */
    @GetMapping
    public ResponseEntity<?> getAllPackages() {
        GetAllPackagesResult result = getAllPackagesUseCase.execute();
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPackages());
        } else {
            return ResponseEntity.internalServerError().body(result.getErrorMessage());
        }
    }

    /**
     * Endpoint para buscar pacotes de um usuário específico
     * Invoca a função get_user_packages do contrato Stellar
     * 
     * @param userAddress Endereço Stellar do usuário
     * @return Lista de pacotes do usuário ou erro
     */
    @GetMapping("/user/{userAddress}")
    public ResponseEntity<?> getUserPackages(@PathVariable String userAddress) {
        try {
            List<Object> userPackages = sorobanContractService.getUserPackages(userAddress);
            return ResponseEntity.ok(userPackages);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao buscar pacotes do usuário: " + e.getMessage());
        }
    }
}