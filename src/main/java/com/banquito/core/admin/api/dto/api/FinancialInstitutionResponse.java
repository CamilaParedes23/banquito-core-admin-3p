package com.banquito.core.admin.api.dto.api;

public record FinancialInstitutionResponse(
        String routingCode,
        String name,
        String accountPrefix,
        Boolean banquito,
        String status
) {}
