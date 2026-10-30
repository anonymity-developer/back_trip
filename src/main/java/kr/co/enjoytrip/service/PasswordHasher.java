package kr.co.enjoytrip.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 210_000;

    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String encoded) {
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < 100_000 || rounds > 1_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, rounds));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static String temporaryPassword() {
        byte[] random = new byte[18];
        RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private static byte[] derive(String password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }
}
