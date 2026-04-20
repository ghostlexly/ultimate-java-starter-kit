package com.lunisoft.javastarter.module.demo.usecase.searchcustomer;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo use case: searches customers by account role. Illustrates how to query across a join
 * (Customer -> Account).
 */
@Service
@RequiredArgsConstructor
public class DemoSearchCustomerUseCase {

  private final DemoCustomerRepository demoCustomerRepository;

  @Transactional(readOnly = true)
  public List<DemoSearchCustomerResult> execute(Role role) {

    return demoCustomerRepository.findByAccountRole(role).stream()
        .map(
            customer ->
                new DemoSearchCustomerResult(
                    customer.getId(),
                    customer.getAccount().getEmail(),
                    customer.getAccount().getRole().name()))
        .toList();
  }
}
