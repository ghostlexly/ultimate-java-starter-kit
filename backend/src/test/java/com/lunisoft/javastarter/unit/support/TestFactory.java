package com.lunisoft.javastarter.unit.support;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.admin.entity.Admin;
import com.lunisoft.javastarter.module.auth.entity.Session;
import com.lunisoft.javastarter.module.auth.entity.VerificationToken;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Shared factory methods for creating test entities. Avoids duplicating helper methods across test
 * classes.
 */
public final class TestFactory {

    // ── Account ──────────────────────────────────────────────

    public static Account createCustomerAccount() {
        var account = new Account("contact+customer@lunisoft.fr", Role.CUSTOMER);
        account.setId(UUID.randomUUID());

        var customer = createCustomer(account);
        account.setCustomer(customer);

        return account;
    }

    public static Account createAdminAccount() {
        var account = new Account("contact+admin@lunisoft.fr", Role.ADMIN);
        account.setId(UUID.randomUUID());

        var admin = new Admin(account);
        account.setAdmin(admin);

        return account;
    }

    // ── Customer ─────────────────────────────────────────────

    public static Customer createCustomer(Account account) {
        var customer = new Customer(account);
        customer.setId(UUID.randomUUID());

        return customer;
    }

    // ── Session ──────────────────────────────────────────────

    public static Session createSession(Account account) {
        var session = new Session(account, Instant.now().plus(7, ChronoUnit.DAYS));
        session.setId(UUID.randomUUID());

        return session;
    }

    // ── VerificationToken ────────────────────────────────────

    public static VerificationToken createVerificationToken(Account account, String code, int attempts) {
        var token = new VerificationToken(
                UUID.randomUUID().toString(),
                VerificationType.LOGIN_CODE,
                account,
                Instant.now().plus(15, ChronoUnit.MINUTES));
        token.setId(UUID.randomUUID());
        token.setValue(code);
        token.setAttempts(attempts);

        return token;
    }
}
