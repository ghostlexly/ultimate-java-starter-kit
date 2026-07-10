package com.lunisoft.javastarter.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record ErrorResponse(
        String type,
        String message,
        String code,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Violation> violations) {}
