package com.lunisoft.ultimatejavastarterkit.module.customer;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.entity.Account;
import com.lunisoft.ultimatejavastarterkit.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.RegisterCustomerRequest;
import com.lunisoft.ultimatejavastarterkit.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final AccountRepository accountRepository;

  public CustomerService(
      CustomerRepository customerRepository, AccountRepository accountRepository) {
    this.customerRepository = customerRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public Customer createProfile(UUID accountId, RegisterCustomerRequest request) {
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

    return customerRepository.save(customer);
  }

  public Customer getProfile(UUID accountId) {
    return customerRepository
        .findByAccountId(accountId)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "Customer profile not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));
  }
}
