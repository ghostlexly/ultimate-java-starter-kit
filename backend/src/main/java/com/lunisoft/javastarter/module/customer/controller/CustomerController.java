package com.lunisoft.javastarter.module.customer.controller;

import com.lunisoft.javastarter.core.security.UserPrincipal;
import com.lunisoft.javastarter.module.customer.dto.request.UpdateCustomerEmailRequest;
import com.lunisoft.javastarter.module.customer.usecase.GetProfileUseCase;
import com.lunisoft.javastarter.module.customer.usecase.UpdateCustomerEmailUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final GetProfileUseCase getProfileUseCase;
    private final UpdateCustomerEmailUseCase updateCustomerEmailUseCase;

    @GetMapping("profile")
    public ResponseEntity<GetProfileUseCase.Output> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        var output = this.getProfileUseCase.execute(principal.accountId());

        return ResponseEntity.ok(output);
    }

    @PatchMapping("email")
    public ResponseEntity<UpdateCustomerEmailUseCase.Output> updateEmail(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody UpdateCustomerEmailRequest request) {

        var output = this.updateCustomerEmailUseCase.execute(principal.accountId(), request.email());

        return ResponseEntity.ok(output);
    }
}
