package com.lunisoft.ultimatejavastarterkit.module.demo.controller;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.demo.dto.SearchCustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.demo.usecase.SearchCustomerUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Validated // Validates request parameters only (countryCode and role) (note: doesn't validate body)
@RequestMapping("/api/demo")
public class DemoController {

  private final SearchCustomerUseCase searchCustomerUseCase;

  /**
   * Demo endpoint: search customers by country code and account role. Example: GET
   * /api/demo/customers?countryCode=FR&role=CUSTOMER
   */
  @GetMapping("/customers")
  public ResponseEntity<List<SearchCustomerResponse>> searchCustomers(
      @Length(min = 2, max = 2) @RequestParam String countryCode, @RequestParam Role role) {

    List<SearchCustomerResponse> results = searchCustomerUseCase.execute(countryCode, role);

    return ResponseEntity.ok(results);
  }
}
