package com.lunisoft.javastarter.module.customer.controller;

import com.lunisoft.javastarter.core.security.UserPrincipal;
import com.lunisoft.javastarter.module.customer.dto.UpdateCustomerEmailRequest;
import com.lunisoft.javastarter.module.customer.usecase.GetProfileUseCase;
import com.lunisoft.javastarter.module.customer.usecase.GetProfileUseCase.GetProfileQuery;
import com.lunisoft.javastarter.module.customer.usecase.GetProfileUseCase.GetProfileResult;
import com.lunisoft.javastarter.module.customer.usecase.UpdateCustomerEmailUseCase;
import com.lunisoft.javastarter.module.customer.usecase.UpdateCustomerEmailUseCase.UpdateCustomerEmailCommand;
import com.lunisoft.javastarter.module.customer.usecase.UpdateCustomerEmailUseCase.UpdateCustomerEmailResult;
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
    public ResponseEntity<GetProfileResult> getProfile(@AuthenticationPrincipal UserPrincipal principal) {

        var query = new GetProfileQuery(principal.accountId());

        var result = this.getProfileUseCase.execute(query);

        return ResponseEntity.ok(result);
    }

    @PatchMapping("email")
    public ResponseEntity<UpdateCustomerEmailResult> updateEmail(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody UpdateCustomerEmailRequest request) {

        var command = new UpdateCustomerEmailCommand(principal.accountId(), request.email());

        var result = this.updateCustomerEmailUseCase.execute(command);

        return ResponseEntity.ok(result);
    }
}
