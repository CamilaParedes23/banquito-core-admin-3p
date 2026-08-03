package com.banquito.core.admin.api.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FinancialInstitutionRequest(
        @NotBlank @Size(max = 20) String routingCode,
        @NotBlank @Size(max = 150) String name,
        @Pattern(regexp = "^$|^[0-9]{5,10}$", message = "accountPrefix debe contener entre 5 y 10 dígitos")
        String accountPrefix,
        Boolean banquito,
        String status
) {}
