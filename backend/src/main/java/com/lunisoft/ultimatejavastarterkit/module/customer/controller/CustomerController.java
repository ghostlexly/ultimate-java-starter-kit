package com.lunisoft.ultimatejavastarterkit.module.customer.controller;

import com.lunisoft.ultimatejavastarterkit.core.security.UserPrincipal;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.CustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.UpdateCustomerEmailRequest;
import com.lunisoft.ultimatejavastarterkit.module.customer.usecase.GetProfileUseCase;
import com.lunisoft.ultimatejavastarterkit.module.customer.usecase.UpdateCustomerEmailUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

  private final GetProfileUseCase getProfileUseCase;
  private final UpdateCustomerEmailUseCase updateCustomerEmailUseCase;

  @GetMapping("/profile")
  public ResponseEntity<CustomerResponse> getProfile(
      @AuthenticationPrincipal UserPrincipal principal) {

    CustomerResponse response = getProfileUseCase.execute(principal.accountId());

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/email")
  public ResponseEntity<CustomerResponse> updateEmail(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody UpdateCustomerEmailRequest request) {

    CustomerResponse response = updateCustomerEmailUseCase.execute(principal.accountId(), request);

    return ResponseEntity.ok(response);
  }
}
