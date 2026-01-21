package net.renars.orbital.api;

import org.springframework.http.ResponseEntity;

public interface Controller {
    default ResponseEntity<String> badRequest(String message) {
        return ResponseEntity.badRequest().body(message);
    }

    default ResponseEntity<String> internalServerError(String message) {
        return ResponseEntity.status(500).body(message);
    }

    default ResponseEntity<String> ok(String message) {
        return ResponseEntity.ok(message);
    }
}
