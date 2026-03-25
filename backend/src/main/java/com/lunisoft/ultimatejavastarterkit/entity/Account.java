package com.lunisoft.ultimatejavastarterkit.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(
    name = "account",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"provider_id", "provider_account_id", "role"})
    })
public class Account extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String email;

  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(name = "provider_id")
  private String providerId;

  @Column(name = "provider_account_id")
  private String providerAccountId;

  @Column(name = "is_email_verified", nullable = false)
  private boolean emailVerified = false;

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Session> sessions;

  @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Customer customer;

  @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Admin admin;

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<VerificationToken> verificationTokens;

  // --- Getters & Setters ---

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getProviderAccountId() {
    return providerAccountId;
  }

  public void setProviderAccountId(String providerAccountId) {
    this.providerAccountId = providerAccountId;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public List<Session> getSessions() {
    return sessions;
  }

  public void setSessions(List<Session> sessions) {
    this.sessions = sessions;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public Admin getAdmin() {
    return admin;
  }

  public void setAdmin(Admin admin) {
    this.admin = admin;
  }

  public List<VerificationToken> getVerificationTokens() {
    return verificationTokens;
  }

  public void setVerificationTokens(List<VerificationToken> verificationTokens) {
    this.verificationTokens = verificationTokens;
  }
}
