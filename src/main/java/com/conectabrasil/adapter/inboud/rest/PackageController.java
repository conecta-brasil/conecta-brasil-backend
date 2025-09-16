package com.conectabrasil.adapter.inboud.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.conectabrasil.application.usecase.GetAllPackagesUseCase;
import com.conectabrasil.application.usecase.GetAllPackagesUseCase.GetAllPackagesResult;

@RestController
@RequestMapping("/packages")
public class PackageController {

    private final GetAllPackagesUseCase getAllPackagesUseCase;

    public PackageController(GetAllPackagesUseCase getAllPackagesUseCase) {
        this.getAllPackagesUseCase = getAllPackagesUseCase;
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
}