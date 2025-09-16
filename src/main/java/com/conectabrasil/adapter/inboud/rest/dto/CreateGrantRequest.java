package com.conectabrasil.adapter.inboud.rest.dto;

public record CreateGrantRequest(String callerUserId, String ownerUserId, String orderId) {
}