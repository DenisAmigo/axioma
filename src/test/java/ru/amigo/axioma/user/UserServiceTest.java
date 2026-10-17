package ru.amigo.axioma.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.amigo.axioma.user.dto.CreateUserRequest;
import ru.amigo.axioma.user.dto.UserResponse;
import ru.amigo.axioma.user.exception.UserAlreadyExistsException;
import ru.amigo.axioma.user.exception.UserNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 23:23
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Role clientRole;
    private CreateUserRequest validRequest;

    private static final String TEST_USERNAME = "user_test";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "testPassword123";
    private static final String TEST_FULL_NAME = "John Doe";

    @BeforeEach
    void setUp() {
        clientRole = new Role("CLIENT", "Клиент");
        validRequest = new CreateUserRequest(
                TEST_USERNAME,
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_FULL_NAME
        );
    }

    @Test
    @DisplayName("Создание пользователя: успех")
    void create_shouldCreateUser_whenDataIsValid() {
        // given
        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("hashed_password");
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));

        // Мокаем сохранение: возвращаем тот же объект, но с ID
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // Эмуляция установки ID
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
            return user;
        });

        // when
        UserResponse response = userService.create(validRequest);

        // then
        assertNotNull(response);
        assertEquals(TEST_USERNAME, response.username());
        assertEquals(TEST_EMAIL, response.email());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
    }

    @Test
    @DisplayName("Создание пользователя: username уже занят")
    void create_shouldThrowException_whenUsernameExists() {
        // given
        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

        // when & then
        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.create(validRequest)
        );

        assertTrue(ex.getMessage().contains(TEST_USERNAME));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение пользователя: успех")
    void getById_shouldReturnUser_whenExists() {
        // given
        User user = new User(TEST_USERNAME, TEST_EMAIL, "hash", TEST_FULL_NAME);
        // Эмуляция ID через рефлексию (в реальном тесте лучше через ReflectionTestUtils)
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        UserResponse response = userService.getById(1L);

        // then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(TEST_USERNAME, response.username());
    }

    @Test
    @DisplayName("Получение пользователя: не найден")
    void getById_shouldThrowException_whenNotFound() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> userService.getById(999L));
    }
}
