package com.lunisoft.ultimatejavastarterkit.module.demo.controller;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.demo.dto.DemoPaginatedCustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.demo.dto.DemoSearchCustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.demo.usecase.DemoPaginateCustomerUseCase;
import com.lunisoft.ultimatejavastarterkit.module.demo.usecase.DemoSearchCustomerUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
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

  private final DemoSearchCustomerUseCase demoSearchCustomerUseCase;
  private final DemoPaginateCustomerUseCase demoPaginateCustomerUseCase;

  /**
   * Demo endpoint: search customers by country code and account role. Example: GET
   * /api/demo/customers?countryCode=FR&role=CUSTOMER
   */
  @GetMapping("/customers")
  public ResponseEntity<List<DemoSearchCustomerResponse>> searchCustomers(
      @Length(min = 2, max = 2) @RequestParam String countryCode, @RequestParam Role role) {

    List<DemoSearchCustomerResponse> results = demoSearchCustomerUseCase.execute(countryCode, role);

    return ResponseEntity.ok(results);
  }

  /**
   * Demo endpoint: paginated list of customers with optional filters. Examples: GET
   * /api/demo/customers/paginated GET /api/demo/customers/paginated?page=1&size=10 GET
   * /api/demo/customers/paginated?email=john GET /api/demo/customers/paginated?countryCode=FR GET
   * /api/demo/customers/paginated?email=john&countryCode=FR&page=1&size=5
   */
  @GetMapping("/customers/paginated")
  public ResponseEntity<DemoPaginatedCustomerResponse> paginateCustomers(
      @Min(1) @RequestParam(defaultValue = "1") int page,
      @Min(1) @Max(100) @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String countryCode) {

    // Convert 1-based page (user-facing) to 0-based (Spring Data internal)
    DemoPaginatedCustomerResponse response =
        demoPaginateCustomerUseCase.execute(page - 1, size, email, countryCode);

    return ResponseEntity.ok(response);
  }

  @GetMapping("simple-json-response")
  public ResponseEntity<Map<String, String>> simpleJsonResponse() {
    return ResponseEntity.ok(Map.of("message", "Success"));
  }

  @GetMapping("simple-message-response")
  public ResponseEntity<String> simpleMessageResponse() {
    return ResponseEntity.ok("Success");
  }
}
