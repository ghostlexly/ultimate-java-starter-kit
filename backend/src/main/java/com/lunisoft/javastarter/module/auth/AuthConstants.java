package com.lunisoft.javastarter.module.auth;

public final class AuthConstants {

  private AuthConstants() {}

  public static final int LOGIN_CODE_COOLDOWN_SECONDS = 60;
  public static final int LOGIN_CODE_EXPIRATION_MINUTES = 15;
  public static final int LOGIN_CODE_MAX_ATTEMPTS = 5;

  public static final String ACCESS_TOKEN_COOKIE = "lunisoft_access_token";
  public static final String REFRESH_TOKEN_COOKIE = "lunisoft_refresh_token";
  public static final int ACCESS_TOKEN_MAX_AGE = 15 * 60;
  public static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;
}
