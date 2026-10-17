package ru.amigo.axioma.user.dto;

import ru.amigo.axioma.user.Role;
import ru.amigo.axioma.user.User;
import ru.amigo.axioma.user.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 2:16
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        UserStatus status,
        Set<String> roles,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
