package com.lunisoft.ultimatejavastarterkit.module.customer.usecase;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.CustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetProfileUseCase {

  private final CustomerRepository customerRepository;

  public GetProfileUseCase(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public CustomerResponse execute(UUID accountId) {
    Customer customer =
        customerRepository
            .findByAccountId(accountId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Customer profile not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

    return new CustomerResponse(
        customer.getId(), customer.getAccount().getEmail());
  }
}
