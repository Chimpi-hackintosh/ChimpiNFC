package com.chimpihackintosh.chimpinfc;
import org.junit.Test;
import static org.junit.Assert.*;
public class SupportVersionsTest {
    @Test public void comparesNumericVersionsAndIgnoresBuildSuffix() {
        assertTrue(SupportVersions.older("0.1.0", "0.1.1"));
        assertFalse(SupportVersions.older("0.1.0", "0.1.0-build-3"));
        assertFalse(SupportVersions.older("1.10.0", "1.9.9"));
        assertFalse(SupportVersions.older("invalid", "1.0.0"));
        assertNull(SupportVersions.parse("1.0"));
        assertNull(SupportVersions.parse("999999999999999.0.0"));
    }
    @Test public void cutoffIsMidnightUtcOnNovemberFourth2026() {
        assertFalse(SupportVersions.ended(1793750399999L));
        assertTrue(SupportVersions.ended(1793750400000L));
    }
}
