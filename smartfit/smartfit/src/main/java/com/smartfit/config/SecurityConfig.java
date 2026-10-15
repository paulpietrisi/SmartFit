package com.smartfit.config;

import com.smartfit.entity.User;
import com.smartfit.repository.UserRepository;
import com.smartfit.service.AuditLogService;
import com.smartfit.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                           UserRepository userRepository,
                           AuditLogService auditLogService) {
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class) // ← FIX
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/logout"),
                                new AntPathRequestMatcher("/checkout/stripe/**"),
                                new AntPathRequestMatcher("/academie/enroll/**"),
                                new AntPathRequestMatcher("/academie/quiz/submit"),
                                new AntPathRequestMatcher("/api/chat")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/static/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers(
                                "/", "/home", "/login", "/register",
                                "/magazin", "/academie"
                        ).permitAll()
                        .requestMatchers(
                                "/checkout/**",
                                "/order-success/**"
                        ).authenticated()
                        // ═══ Panou ADMIN — exclusiv ROLE_ADMIN ═══
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // ═══ Panou ANGAJAT — ROLE_EMPLOYEE sau ROLE_ADMIN (adminul poate inspecta) ═══
                        .requestMatchers("/angajat/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        // ═══ Rapoarte generale — admin + angajați ═══
                        .requestMatchers("/rapoarte", "/setari").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")    // <--- ADAUGĂ LINIA ASTA
                        .passwordParameter("password")   // <--- (Opțional, dar recomandat)
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .addLogoutHandler(logoutAuditHandler())
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // Pagină personalizată de "Acces interzis" (403) — vezi /eroare/403
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/eroare/403")
                );

        return http.build();
    }

    /**
     * După login reușit: loghează acțiunea LOGIN și redirecționează în funcție de rol:
     * - ADMIN    → /admin/dashboard
     * - EMPLOYEE → /angajat/dashboard
     * - USER     → /dashboard
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email);

            if (user != null) {
                auditLogService.log(user, "LOGIN", "Autentificare reușită", null, null, null, request);
            }

            String redirectUrl = "/dashboard";
            if (user != null) {
                if ("ADMIN".equals(user.getRole())) {
                    redirectUrl = "/admin/dashboard";
                } else if ("EMPLOYEE".equals(user.getRole())) {
                    redirectUrl = "/angajat/dashboard";
                }
            }

            response.sendRedirect(redirectUrl);
        };
    }

    /**
     * La logout: loghează acțiunea LOGOUT înainte de invalidarea sesiunii.
     */
    @Bean
    public LogoutHandler logoutAuditHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email);
                if (user != null) {
                    auditLogService.log(user, "LOGOUT", "Deconectare", null, null, null, request);
                }
            }
        };
    }

    // Forțează încărcarea tokenului CSRF pe fiecare request
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken(); // forțează generarea și scrierea cookie-ului
            }
            filterChain.doFilter(request, response);
        }
    }
}
