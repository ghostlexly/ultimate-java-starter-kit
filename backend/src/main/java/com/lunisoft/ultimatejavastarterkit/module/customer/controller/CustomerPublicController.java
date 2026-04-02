package com.lunisoft.ultimatejavastarterkit.module.customer.controller;

import com.lunisoft.ultimatejavastarterkit.core.security.PublicEndpoint;
import com.lunisoft.ultimatejavastarterkit.core.security.UserPrincipal;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.CustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.RegisterCustomerRequest;
import com.lunisoft.ultimatejavastarterkit.module.customer.usecase.CreateProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@PublicEndpoint
@RestController
@RequestMapping("/api/customer")
public class CustomerPublicController {

  private final CreateProfileUseCase createProfileUseCase;

  public CustomerPublicController(CreateProfileUseCase createProfileUseCase) {
    this.createProfileUseCase = createProfileUseCase;
  }

  @PostMapping("/profile")
  public ResponseEntity<CustomerResponse> createProfile(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody RegisterCustomerRequest request) {

    CustomerResponse response = createProfileUseCase.execute(principal.accountId(), request);

    return ResponseEntity.ok(response);
  }
}
