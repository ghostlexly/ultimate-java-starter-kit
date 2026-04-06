package com.lunisoft.ultimatejavastarterkit.module.demo.usecase;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.demo.dto.DemoSearchCustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.demo.repository.DemoCustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo use case: searches customers by account role.
 * Illustrates how to query across a join (Customer -> Account).
 */
@Service
public class DemoSearchCustomerUseCase {

  private final DemoCustomerRepository demoCustomerRepository;

  public DemoSearchCustomerUseCase(DemoCustomerRepository demoCustomerRepository) {
    this.demoCustomerRepository = demoCustomerRepository;
  }

  @Transactional(readOnly = true)
  public List<DemoSearchCustomerResponse> execute(Role role) {

    return demoCustomerRepository
        .findByAccountRole(role)
        .stream()
        .map(customer -> new DemoSearchCustomerResponse(
            customer.getId(),
            customer.getAccount().getEmail(),
            customer.getAccount().getRole().name()))
        .toList();
  }
}
