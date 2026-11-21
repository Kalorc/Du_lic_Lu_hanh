package vn.ptit.tour.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import vn.ptit.tour.entity.AppUser;
import vn.ptit.tour.entity.Role;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test void absentSecretUsesIndependentEphemeralKeys() {
        JwtService first = assertDoesNotThrow(() -> new JwtService("", 480));
        JwtService second = new JwtService("", 480);
        AppUser user = new AppUser();
        user.setUserId(1L); user.setUsername("qa-product"); user.setRole(Role.PRODUCT);
        user.setFullName("QA Product");
        String token = first.generate(user);
        assertEquals("qa-product", first.parse(token).getSubject());
        assertThrows(JwtException.class, () -> second.parse(token));
    }
}
