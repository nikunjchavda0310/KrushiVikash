package com.farm.Config;

import com.farm.Services.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomSuccessHandler successHandler;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/uploads/**", "/images/**", "/css/**", "/js/**", "/error");
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/index", "/about", "/login",
                                "/verify", "/verify-otp", "/admin/reg/**").permitAll()

                        .requestMatchers("/farmer/**").hasAnyAuthority("ROLE_FARMER", "FARMER")
                        .requestMatchers("/client/**").hasAnyAuthority("ROLE_CLIENT", "CLIENT")
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("pass")
                        .successHandler(successHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .expiredUrl("/login?expired=true")
                );

        return http.build();
    }

    /**
     * Smart Access Denied Handler
     * Detects if a user is logged in but tries to access the wrong panel,
     * then redirects them back to their own dashboard.
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null) {
                Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
                System.out.println("Access Denied for user: " + auth.getName() + " with roles: " + roles);

                if (roles.contains("ROLE_ADMIN") || roles.contains("ADMIN")) {
                    response.sendRedirect(request.getContextPath() + "/admin/index?error=noAccess");
                    return;
                } else if (roles.contains("ROLE_FARMER") || roles.contains("FARMER")) {
                    response.sendRedirect(request.getContextPath() + "/farmer/index?error=noAccess");
                    return;
                }
            }
            response.sendRedirect(request.getContextPath() + "/login?accessDenied=true");
        };
    }
}