package net.renars.orbital.api;

import org.springframework.http.ResponseEntity;

public interface Controller {
    default <T> ResponseEntity<T> badRequest(T message) {
        return ResponseEntity.badRequest().body(message);
    }

    default <T> ResponseEntity<T> internalServerError(T message) {
        return ResponseEntity.status(500).body(message);
    }

    default <T> ResponseEntity<T> ok(T message) {
        return ResponseEntity.ok(message);
    }
}
