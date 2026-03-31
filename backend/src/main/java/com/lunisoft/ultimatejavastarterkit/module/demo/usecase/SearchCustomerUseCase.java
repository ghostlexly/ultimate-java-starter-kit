package com.lunisoft.ultimatejavastarterkit.module.demo.usecase;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.demo.dto.SearchCustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.demo.repository.DemoCustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo use case: searches customers by country code and account role.
 * Illustrates how to query across a join (Customer -> Account).
 */
@RequiredArgsConstructor
@Service
public class SearchCustomerUseCase {

  private final DemoCustomerRepository demoCustomerRepository;

  @Transactional(readOnly = true)
  public List<SearchCustomerResponse> execute(String countryCode, Role role) {
    return demoCustomerRepository
        .findByCountryCodeAndAccountRole(countryCode, role)
        .stream()
        .map(customer -> new SearchCustomerResponse(
            customer.getId(),
            customer.getAccount().getEmail(),
            customer.getCountryCode(),
            customer.getAccount().getRole().name()))
        .toList();
  }
}
