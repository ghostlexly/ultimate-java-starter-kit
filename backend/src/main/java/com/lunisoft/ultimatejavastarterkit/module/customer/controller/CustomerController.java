package com.lunisoft.ultimatejavastarterkit.module.customer.controller;

import com.lunisoft.ultimatejavastarterkit.core.security.UserPrincipal;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.CustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.RegisterCustomerRequest;
import com.lunisoft.ultimatejavastarterkit.module.customer.usecase.CreateProfileUseCase;
import com.lunisoft.ultimatejavastarterkit.module.customer.usecase.GetProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

  private final CreateProfileUseCase createProfileUseCase;
  private final GetProfileUseCase getProfileUseCase;

  public CustomerController(CreateProfileUseCase createProfileUseCase, GetProfileUseCase getProfileUseCase) {
    this.createProfileUseCase = createProfileUseCase;
    this.getProfileUseCase = getProfileUseCase;
  }

  @PostMapping("/profile")
  public ResponseEntity<CustomerResponse> createProfile(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody RegisterCustomerRequest request) {

    CustomerResponse response = createProfileUseCase.execute(principal.accountId(), request);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/profile")
  public ResponseEntity<CustomerResponse> getProfile(
      @AuthenticationPrincipal UserPrincipal principal) {

    CustomerResponse response = getProfileUseCase.execute(principal.accountId());

    return ResponseEntity.ok(response);
  }
}
