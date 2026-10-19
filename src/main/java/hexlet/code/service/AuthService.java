package hexlet.code.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public String login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String normalized = username.trim().toLowerCase();
        var authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(normalized, password));

        if (!(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return jwtService.generateToken(principal.id(), principal.email());
    }
}
