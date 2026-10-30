package kr.co.enjoytrip.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AppConfigTest {
    @Test void portalKeysAcceptRawAndAlreadyEncodedValues() {
        assertEquals("ab%2B%2F%3D", AppConfig.encodePortalKeyValue("ab+/="));
        assertEquals("ab%2B%2F%3D", AppConfig.encodePortalKeyValue("ab%2B%2F%3D"));
    }
}
