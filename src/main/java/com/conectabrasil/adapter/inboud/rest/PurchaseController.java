package com.conectabrasil.adapter.inboud.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.conectabrasil.adapter.inboud.rest.dto.CreatePurchaseRequest;
import com.conectabrasil.adapter.inboud.rest.dto.PurchaseResponse;
import com.conectabrasil.application.usecase.CreatePurchaseUseCase;

@RestController
@RequestMapping
public class PurchaseController {
    private final CreatePurchaseUseCase createPurchase;

    public PurchaseController(CreatePurchaseUseCase createPurchase) {
        this.createPurchase = createPurchase;
    }

    @PostMapping("/purchases")
    public PurchaseResponse create(@RequestBody CreatePurchaseRequest req) {
        var result = createPurchase.execute(req.userId(), req.packageId());
        return PurchaseResponse.from(result.getPurchase(), result.getUnsignedXdr());
    }
}
