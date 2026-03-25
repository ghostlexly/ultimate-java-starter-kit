package com.lunisoft.ultimatejavastarterkit.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for domain/business logic violations. Produces a structured error response with a
 * machine-readable code.
 */
public class BusinessRuleException extends RuntimeException {

  private final String code;
  private final HttpStatus status;

  public BusinessRuleException(String message, String code, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
