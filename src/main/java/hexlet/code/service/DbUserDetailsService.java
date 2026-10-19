package hexlet.code.service;

import hexlet.code.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DbUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }
        String normalized = username.trim().toLowerCase();
        return userRepository
                .findByEmail(normalized)
                .<UserDetails>map(
                        user ->
                                new AppUserDetails(
                                        user.getId(), user.getEmail(), user.getPasswordDigest()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));
    }
}
