package hexlet.code.service;

import hexlet.code.dto.UserCreateDTO;
import hexlet.code.dto.UserDTO;
import hexlet.code.dto.UserUpdateDTO;
import hexlet.code.exception.DuplicateEmailException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.UserMapper;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JsonMapper jsonMapper,
            Validator validator) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    public Page<UserDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
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
        User user = getUserOrThrow(id);

        JsonNode currentJson = jsonMapper.valueToTree(user);
        JsonNode patchedJson = jsonMapper.updateValue(currentJson, patchNode);
        UserUpdateDTO dto = jsonMapper.treeToValue(patchedJson, UserUpdateDTO.class);

        // Bean Validation: @NotBlank email, @Email email, @Size password
        Set<ConstraintViolation<UserUpdateDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<UserUpdateDTO> v : violations) {
                errors.put(v.getPropertyPath().toString(), v.getMessage());
            }
            throw new ValidationException(errors);
        }

        // Явный null для password → 400 (у email это уже сделал @NotBlank)
        if (patchNode.has("password") && patchNode.get("password").isNull()) {
            throw new ValidationException(Map.of("password", "Password is required"));
        }

        // Email: проверка дубликата + нормализация
        if (dto.getEmail() != null) {
            String normalized = normalize(dto.getEmail());
            if (!normalized.equals(user.getEmail()) && userRepository.existsByEmail(normalized)) {
                throw new DuplicateEmailException(
                        "User with email " + normalized + " already exists");
            }
            user.setEmail(normalized);
        }

        // Пароль: обновляем digest только если поле пришло
        if (dto.getPassword() != null) {
            user.setPasswordDigest(passwordEncoder.encode(dto.getPassword()));
        }

        // firstName / lastName — маппер, включая null
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
