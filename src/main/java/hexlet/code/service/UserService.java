package hexlet.code.service;

import hexlet.code.dto.UserCreateDTO;
import hexlet.code.dto.UserDTO;
import hexlet.code.dto.UserUpdateDTO;
import hexlet.code.exception.DuplicateEmailException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.mapper.UserMapper;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UserDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toDto);
    }

    public UserDTO findById(Long id) {
        return userMapper.toDto(getUserOrThrow(id));
    }

    @Transactional
    public UserDTO create(UserCreateDTO dto) {
        String email = normalize(dto.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("User with email " + email + " already exists");
        }

        User user = userMapper.toEntity(dto);
        user.setEmail(email);
        user.setPasswordDigest(passwordEncoder.encode(dto.password()));
        user.setCreatedAt(LocalDateTime.now());

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDTO update(Long id, UserUpdateDTO dto) {
        User user = getUserOrThrow(id);

        if (dto.getPassword().isPresent()) {
            user.setPasswordDigest(passwordEncoder.encode(dto.getPassword().orElse(null)));
        }

        userMapper.update(dto, user);

        if (dto.getEmail().isPresent() && user.getEmail() != null) {
            String normalized = normalize(user.getEmail());

            if (!normalized.equals(user.getEmail()) && userRepository.existsByEmail(normalized)) {
                throw new DuplicateEmailException("User with email " + normalized + " already exists");
            }

            user.setEmail(normalized);
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = getUserOrThrow(id);
        userRepository.delete(user);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}