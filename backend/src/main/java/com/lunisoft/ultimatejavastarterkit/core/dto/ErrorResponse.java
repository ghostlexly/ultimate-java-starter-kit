package com.lunisoft.ultimatejavastarterkit.core.dto;

import java.util.List;

public record ErrorResponse(String type, String message, String code, List<Violation> violations) {}
