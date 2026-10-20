package ru.amigo.axioma.idempotency;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.amigo.axioma.user.User;
import ru.amigo.axioma.user.UserRepository;
import ru.amigo.axioma.user.exception.UserNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 17:17
 */
@Service
@Transactional(readOnly = true)
public class IdempotencyKeyService {

    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository repository;
    private final UserRepository userRepository;

    public IdempotencyKeyService(IdempotencyKeyRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public Optional<IdempotencyKey> findByKeyAndUser(String key, Long userId) {
        return repository.findById(key)
                .filter(k -> k.getUser().getId().equals(userId));
    }

    @Transactional
    public IdempotencyKey save(String key, Long userId, String requestHash, String responseBody, int responseStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        IdempotencyKey entity = new IdempotencyKey(
                key,
                user,
                requestHash,
                Instant.now().plus(TTL)
        );
        entity.setResponseBody(responseBody);
        entity.setResponseStatus(responseStatus);
        return repository.save(entity);
    }
}
