package com.cou.bustracker.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression tests for the shared {@link PhoneUtils#normalizeBd(String)}
 * helper. The registration and login services previously each had their own
 * normalize method, and they disagreed on what to do with a 10-digit
 * input (e.g. {@code "1793165308"}). Login added the missing leading zero,
 * registration did not — a freshly-created user could not log back in.
 *
 * <p>These tests pin the canonical 11-digit {@code 01XXXXXXXXX} output for
 * every input shape we accept from the Flutter app, the legacy web panel,
 * and direct API calls.
 */
class PhoneUtilsTest {

    @Test
    void normalizeBd_handlesNull() {
        assertNull(PhoneUtils.normalizeBd(null));
    }

    @Test
    void normalizeBd_acceptsCanonical11DigitFormat() {
        assertEquals("01793165308", PhoneUtils.normalizeBd("01793165308"));
    }

    @Test
    void normalizeBd_stripsBangladeshCountryCode() {
        assertEquals("01793165308", PhoneUtils.normalizeBd("8801793165308"));
    }

    @Test
    void normalizeBd_stripsPlusAndCountryCode() {
        assertEquals("01793165308", PhoneUtils.normalizeBd("+8801793165308"));
    }

    @Test
    void normalizeBd_stripsSpacesAndDashes() {
        assertEquals("01793165308", PhoneUtils.normalizeBd("+880 1793-165308"));
    }

    /**
     * The regression: 10-digit input (no leading 0) used to be passed through
     * by the registration service and turned into {@code 01793165308} by the
     * login service. They must now agree on the same 11-digit output.
     */
    @Test
    void normalizeBd_prependsLeadingZeroFor10DigitInput() {
        assertEquals("01793165308", PhoneUtils.normalizeBd("1793165308"));
    }

    @Test
    void normalizeBd_trimsWhitespace() {
        assertEquals("01793165308", PhoneUtils.normalizeBd("  01793165308  "));
    }

    @Test
    void normalizeBd_isIdempotent() {
        String once = PhoneUtils.normalizeBd("+880 1793-165308");
        String twice = PhoneUtils.normalizeBd(once);
        assertEquals("01793165308", once);
        assertEquals(once, twice);
    }

    @Test
    void toE164Bd_addsCountryCodeForCanonicalInput() {
        assertEquals("8801793165308", PhoneUtils.toE164Bd("01793165308"));
    }

    @Test
    void toE164Bd_passesThroughAlreadyE164() {
        assertEquals("8801793165308", PhoneUtils.toE164Bd("8801793165308"));
    }

    @Test
    void toE164Bd_handlesNull() {
        assertNull(PhoneUtils.toE164Bd(null));
    }
}
