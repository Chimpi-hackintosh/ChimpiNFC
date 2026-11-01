package com.chimpihackintosh.chimpinfc

import org.junit.Assert.*
import org.junit.Test

class InputValidatorTest {
    @Test fun textAndUrlValid() {
        assertEquals("Meu projeto", InputValidator.normalize("text", " Meu projeto "))
        assertEquals("https://example.org", InputValidator.normalize("url", "https://example.org"))
    }

    @Test fun appleAddressCannotBeCopied() {
        for (url in listOf("https://found.apple.com/", "https://icloud.com/", "https://x.apple.com/")) {
            try {
                InputValidator.normalize("url", url)
                fail("Deveria bloquear: " + url)
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun airtagPublicLinkDetectedPrecisely() {
        assertTrue(InputValidator.isPublicAirTagUrl("https://found.apple.com/airtag"))
        assertFalse(InputValidator.isPublicAirTagUrl("http://found.apple.com/"))
        assertFalse(InputValidator.isPublicAirTagUrl("https://found.apple.com.attacker.net/"))
    }

    @Test fun missingContentRejected() {
        try {
            InputValidator.normalize("text", "   ")
            fail("Texto vazio foi aceito")
        } catch (_: IllegalArgumentException) { }
    }
}
