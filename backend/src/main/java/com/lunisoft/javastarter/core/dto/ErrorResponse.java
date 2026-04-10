package com.lunisoft.javastarter.core.dto;

import java.util.List;

public record ErrorResponse(String type, String message, String code, List<Violation> violations) {}
