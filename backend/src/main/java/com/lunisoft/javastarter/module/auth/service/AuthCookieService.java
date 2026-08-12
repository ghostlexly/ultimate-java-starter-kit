package com.lunisoft.javastarter.module.auth.service;

import com.lunisoft.javastarter.module.auth.dto.RefreshTokenRequest;
import com.lunisoft.javastarter.property.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.lunisoft.javastarter.module.auth.AuthConstants.ACCESS_TOKEN_COOKIE;
import static com.lunisoft.javastarter.module.auth.AuthConstants.REFRESH_TOKEN_COOKIE;

/** Manages authentication cookies (access and refresh tokens). */
@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private final JwtProperties jwtProperties;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    /** Resolves the refresh token from the request body or cookie fallback. */
    public String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        // Check request body first
        if (request != null && request.refreshToken() != null) {
            return request.refreshToken();
        }

        // Fallback to cookie
        if (httpRequest.getCookies() != null) {
            for (Cookie cookie : httpRequest.getCookies()) {
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /** Sets the access and refresh token cookies on the response. */
    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Cookies live exactly as long as the tokens they carry (app.jwt.* expiration minutes).
        long accessTokenExpSeconds =
                Duration.ofMinutes(jwtProperties.accessTokenExpirationMinutes()).toSeconds();
        int accessTokenMaxAge = Math.toIntExact(accessTokenExpSeconds);

        long refreshTokenExpSeconds = Duration.ofMinutes(jwtProperties.refreshTokenExpirationMinutes())
                .toSeconds();
        int refreshTokenMaxAge = Math.toIntExact(refreshTokenExpSeconds);

        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, accessTokenMaxAge);
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, refreshTokenMaxAge);
    }

    /** Clears the access and refresh token cookies. */
    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        var cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookies);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
