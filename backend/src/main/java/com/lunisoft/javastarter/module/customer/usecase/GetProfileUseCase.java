package com.lunisoft.javastarter.module.customer.usecase;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.customer.dto.CustomerResponse;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase {

  private final CustomerRepository customerRepository;

  public CustomerResponse execute(UUID accountId) {
    Customer customer =
        customerRepository
            .findByAccountId(accountId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Customer profile not found", "NOT_FOUND", HttpStatus.NOT_FOUND));

    return new CustomerResponse(customer.getId(), customer.getAccount().getEmail());
  }
}
