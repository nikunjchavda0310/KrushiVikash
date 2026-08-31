package com.farm.Config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // Logging the role to the console for easier debugging
        System.out.println("Login Success! User: " + authentication.getName() + " | Roles: " + roles);

        // 1. Clear session-based authentication exceptions
        clearAuthenticationAttributes(request);

        // 2. Role-based Redirection Logic
        String targetUrl;
        if (roles.contains("ROLE_ADMIN") || roles.contains("ADMIN")) {
            targetUrl = "/admin/index?loginSuccess=true";
        }
        else if (roles.contains("ROLE_FARMER") || roles.contains("FARMER")) {
            targetUrl = "/farmer/index?loginSuccess=true";
        }
        else if (roles.contains("ROLE_CLIENT") || roles.contains("CLIENT")) {
            targetUrl = "/client/index?loginSuccess=true";
        }
        else {
            targetUrl = "/index?loginSuccess=true";
        }

        // 3. Final Redirect
        // Using response.encodeRedirectURL to ensure the session ID is handled correctly
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + targetUrl));
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        // Removes the last login error message so it doesn't reappear
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}