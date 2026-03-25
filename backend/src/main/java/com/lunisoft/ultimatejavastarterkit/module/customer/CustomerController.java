package com.lunisoft.ultimatejavastarterkit.module.customer;

import com.lunisoft.ultimatejavastarterkit.core.security.UserPrincipal;
import com.lunisoft.ultimatejavastarterkit.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.CustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.RegisterCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @PostMapping("/profile")
  public ResponseEntity<CustomerResponse> createProfile(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody RegisterCustomerRequest request) {

    Customer customer = customerService.createProfile(principal.accountId(), request);

    return ResponseEntity.ok(toResponse(customer));
  }

  @GetMapping("/profile")
  public ResponseEntity<CustomerResponse> getProfile(
      @AuthenticationPrincipal UserPrincipal principal) {
    Customer customer = customerService.getProfile(principal.accountId());

    return ResponseEntity.ok(toResponse(customer));
  }

  private CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(
        customer.getId(), customer.getAccount().getEmail(), customer.getCountryCode());
  }
}
