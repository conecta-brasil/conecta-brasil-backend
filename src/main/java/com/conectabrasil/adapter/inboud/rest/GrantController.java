package com.conectabrasil.adapter.inboud.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.conectabrasil.adapter.inboud.rest.dto.CreateGrantRequest;
import com.conectabrasil.adapter.inboud.rest.dto.GrantResponse;
import com.conectabrasil.application.usecase.CreateGrantUseCase;

@RestController
@RequestMapping
public class GrantController {
    private final CreateGrantUseCase createGrant;

    public GrantController(CreateGrantUseCase createGrant) {
        this.createGrant = createGrant;
    }

    @PostMapping("/grants")
    public GrantResponse create(@RequestBody CreateGrantRequest req) {
        var result = createGrant.execute(req.callerUserId(), req.ownerUserId(), req.orderId());
        return GrantResponse.from(result.getGrant(), result.getUnsignedXdr());
    }
}