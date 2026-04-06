package com.lunisoft.ultimatejavastarterkit.module.auth.service;

import static com.lunisoft.ultimatejavastarterkit.module.auth.AuthConstants.*;

import com.lunisoft.ultimatejavastarterkit.module.auth.dto.AuthResponse;
import com.lunisoft.ultimatejavastarterkit.module.auth.dto.RefreshTokenRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Manages authentication cookies (access and refresh tokens).
 */
@Service
public class AuthCookieService {

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    /**
     * Resolves the refresh token from the request body or cookie fallback.
     */
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

    /**
     * Sets the access and refresh token cookies on the response.
     */
    public void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        addCookie(response, ACCESS_TOKEN_COOKIE, authResponse.accessToken(), ACCESS_TOKEN_MAX_AGE);
        addCookie(response, REFRESH_TOKEN_COOKIE, authResponse.refreshToken(), REFRESH_TOKEN_MAX_AGE);
    }

    /**
     * Clears the access and refresh token cookies.
     */
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
