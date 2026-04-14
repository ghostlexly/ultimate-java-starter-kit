package com.lunisoft.javastarter.module.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record BodyValidationExampleRequest(
    @NotBlank @Email String email, @NotBlank String firstName) {}
