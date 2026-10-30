package kr.co.enjoytrip.config;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class AppConfig {
    private AppConfig() {}

    public static String get(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    public static String required(String name) {
        String value = get(name);
        if (value.isEmpty()) throw new IllegalStateException("Missing environment variable: " + name);
        return value;
    }

    public static String encodedPortalKey(String name) {
        return encodePortalKeyValue(required(name));
    }

    static String encodePortalKeyValue(String key) {
        if (key.matches(".*%[0-9A-Fa-f]{2}.*")) {
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
        }
        return URLEncoder.encode(key, StandardCharsets.UTF_8);
    }
}
