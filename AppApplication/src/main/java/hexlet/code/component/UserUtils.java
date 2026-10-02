package hexlet.code.component;

import hexlet.code.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userUtils")
@RequiredArgsConstructor
public class UserUtils {
    private final UserRepository repository;

    public boolean isSelf(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return repository.findById(id)
                .map(user -> user.getEmail().equals(auth.getName()))
                .orElse(true); // missing user: let the service answer 404
    }
}