package com.lunisoft.ultimatejavastarterkit.module.customer.usecase;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.CustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.RegisterCustomerRequest;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateProfileUseCase {

  private final CustomerRepository customerRepository;
  private final AccountRepository accountRepository;

  public CreateProfileUseCase(CustomerRepository customerRepository, AccountRepository accountRepository) {
    this.customerRepository = customerRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public CustomerResponse execute(UUID accountId, RegisterCustomerRequest request) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Account not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

    if (customerRepository.findByAccountId(accountId).isPresent()) {
      throw new BusinessRuleException(
          "Customer profile already exists.", "ALREADY_EXISTS", HttpStatus.CONFLICT);
    }

    Customer customer = new Customer();
    customer.setAccount(account);
    customer.setCountryCode(request.countryCode());
    customerRepository.save(customer);

    return new CustomerResponse(customer.getId(), account.getEmail(), customer.getCountryCode());
  }
}
