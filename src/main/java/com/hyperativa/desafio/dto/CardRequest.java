package com.hyperativa.desafio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d+", message = "Must contain only digits")
    private String cardNumber;
}
