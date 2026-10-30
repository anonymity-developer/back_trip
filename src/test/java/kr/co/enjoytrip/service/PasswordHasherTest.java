package kr.co.enjoytrip.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {
    @Test void saltsAndVerifiesPasswords() {
        String first=PasswordHasher.hash("A-strong-passphrase-2026");
        String second=PasswordHasher.hash("A-strong-passphrase-2026");
        assertNotEquals(first,second);
        assertTrue(PasswordHasher.verify("A-strong-passphrase-2026",first));
        assertFalse(PasswordHasher.verify("wrong",first));
        assertFalse(PasswordHasher.verify("A-strong-passphrase-2026","broken-hash"));
    }
    @Test void temporaryPasswordsAreIndependent() {
        String first=PasswordHasher.temporaryPassword();
        assertTrue(first.length()>=20);
        assertNotEquals(first,PasswordHasher.temporaryPassword());
    }
}
