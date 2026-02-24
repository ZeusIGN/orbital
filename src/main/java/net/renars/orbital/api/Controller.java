package net.renars.orbital.api;

import net.renars.orbital.config.WrappedUserDetails;
import net.renars.orbital.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public interface Controller {
    default <T> ResponseEntity<T> badRequest(T message) {
        return ResponseEntity.badRequest().body(message);
    }

    default <T> ResponseEntity<T> badRequest() {
        return ResponseEntity.badRequest().body(null);
    }

    default <T> ResponseEntity<T> internalServerError(T message) {
        return ResponseEntity.status(500).body(message);
    }

    default <T> ResponseEntity<T> ok(T message) {
        return ResponseEntity.ok(message);
    }

    default Optional<User> getAuthUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(auth != null && auth.getPrincipal() instanceof WrappedUserDetails(User user) ? user : null);
    }
}
