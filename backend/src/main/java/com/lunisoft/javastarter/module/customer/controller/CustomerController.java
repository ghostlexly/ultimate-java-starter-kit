package com.lunisoft.javastarter.module.customer.controller;

import com.lunisoft.javastarter.core.security.UserPrincipal;
import com.lunisoft.javastarter.module.customer.dto.CustomerResponse;
import com.lunisoft.javastarter.module.customer.dto.UpdateCustomerEmailRequest;
import com.lunisoft.javastarter.module.customer.usecase.GetProfileUseCase;
import com.lunisoft.javastarter.module.customer.usecase.UpdateCustomerEmailInput;
import com.lunisoft.javastarter.module.customer.usecase.UpdateCustomerEmailUseCase;
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

  @GetMapping("profile")
  public ResponseEntity<CustomerResponse> getProfile(
      @AuthenticationPrincipal UserPrincipal principal) {

    CustomerResponse response = getProfileUseCase.execute(principal.accountId());

    return ResponseEntity.ok(response);
  }

  @PatchMapping("email")
  public ResponseEntity<CustomerResponse> updateEmail(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody UpdateCustomerEmailRequest request) {

    var input = new UpdateCustomerEmailInput(principal.accountId(), request.email());

    CustomerResponse response = updateCustomerEmailUseCase.execute(input);

    return ResponseEntity.ok(response);
  }
}
