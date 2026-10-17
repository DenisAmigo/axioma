package ru.amigo.axioma.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.amigo.axioma.user.dto.CreateUserRequest;
import ru.amigo.axioma.user.dto.UpdateUserRequest;
import ru.amigo.axioma.user.dto.UserResponse;
import ru.amigo.axioma.user.exception.UserAlreadyExistsException;
import ru.amigo.axioma.user.exception.UserNotFoundException;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 12:18
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("username", request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("email", request.email());
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
        );

        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Роль CLIENT не найдена. Проверьте V1__init.sql"));
        user.addRole(clientRole);

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return UserResponse.from(user);
    }

    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        return UserResponse.from(user);
    }

    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new UserAlreadyExistsException("email", request.email());
            }
            user.setEmail(request.email());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }

        User updated = userRepository.save(user);
        return UserResponse.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }
}
