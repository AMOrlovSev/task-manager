package hexlet.code.service;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.user.UserCreateDTO;
import hexlet.code.dto.user.UserDTO;
import hexlet.code.dto.user.UserUpdateDTO;
import hexlet.code.exception.DuplicateEmailException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.UserMapper;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PatchSupport patchSupport;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            PatchSupport patchSupport) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.patchSupport = patchSupport;
    }

    public List<UserDTO> findAll() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
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

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDTO update(Long id, JsonNode patchNode) {
        patchSupport.requireObject(patchNode);
        User user = getUserOrThrow(id);

        UserUpdateDTO dto =
                patchSupport.apply(userMapper.toUpdateDto(user), patchNode, UserUpdateDTO.class);

        if (patchNode.has("password") && patchNode.get("password").isNull()) {
            throw new ValidationException(Map.of("password", "Password is required"));
        }

        if (dto.getEmail() != null) {
            String normalized = normalize(dto.getEmail());
            if (!normalized.equals(user.getEmail()) && userRepository.existsByEmail(normalized)) {
                throw new DuplicateEmailException(
                        "User with email " + normalized + " already exists");
            }
            user.setEmail(normalized);
        }

        if (dto.getPassword() != null) {
            user.setPasswordDigest(passwordEncoder.encode(dto.getPassword()));
        }

        userMapper.update(dto, user);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = getUserOrThrow(id);
        userRepository.delete(user);
    }

    private User getUserOrThrow(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
