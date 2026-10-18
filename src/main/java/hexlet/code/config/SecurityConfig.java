package hexlet.code.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    @SuppressWarnings("java:S4502") // stateless Bearer API: CSRF не нужен
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/welcome", "/actuator/health/**", "/api/login")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/users")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/task_statuses/**")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/docs",
                                                "/swagger-ui/**",
                                                "/api-docs",
                                                "/api-docs/**",
                                                "/api-docs.yaml")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/",
                                                "/index.html",
                                                "/assets/**",
                                                "/favicon.ico")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }

    // H2 console: отдельная цепочка, иначе её съест 401 или блокировка фрейма
    @Bean
    @Order(1)
    @Profile("development")
    SecurityFilterChain h2ConsoleChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/h2-console/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .build();
    }
}
