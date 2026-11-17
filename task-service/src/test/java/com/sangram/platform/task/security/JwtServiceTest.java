package com.sangram.platform.task.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijklmnop";

    @Test
    void issuedTokenRoundTripsSubjectAndRoles() {
        JwtService service = new JwtService(SECRET, 5);

        Claims claims = service.parse(service.issue("alice", List.of("USER", "ADMIN")));

        assertEquals("alice", claims.getSubject());
        assertEquals(List.of("USER", "ADMIN"), claims.get("roles", List.class));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("another-secret-0123456789-abcdefghijklmnopqrst", 5)
                .issue("mallory", List.of("ADMIN"));

        assertThrows(JwtException.class, () -> new JwtService(SECRET, 5).parse(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, -1);

        assertThrows(JwtException.class, () -> service.parse(service.issue("bob", List.of("USER"))));
    }
}
