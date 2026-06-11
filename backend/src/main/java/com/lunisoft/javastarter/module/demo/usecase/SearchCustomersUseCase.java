package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo use case: searches customers by account role. Illustrates how to query across a join
 * (Customer -> Account).
 */
@Service
@RequiredArgsConstructor
public class SearchCustomersUseCase {

  private final DemoCustomerRepository demoCustomerRepository;

  public record Input(Role role) {}

  public record Output(UUID id, String email, String role) {}

  @Transactional(readOnly = true)
  public List<Output> execute(Input input) {
    return demoCustomerRepository.findByAccountRole(input.role()).stream()
        .map(
            customer ->
                new Output(
                    customer.getId(),
                    customer.getAccount().getEmail(),
                    customer.getAccount().getRole().name()))
        .toList();
  }
}
