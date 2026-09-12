package com.cou.bustracker.util;

/**
 * Shared Bangladeshi phone-number helpers.
 *
 * <p>All phone persistence and lookup on the backend MUST go through
 * {@link #normalizeBd(String)} so that registration and login agree on a
 * single canonical format. Previously the registration and login services
 * each carried their own normalize method, and they disagreed on what to
 * do with a 10-digit input (e.g. {@code "1793165308"}). Login added the
 * missing leading zero, registration did not, so a freshly-created user
 * could not log back in. Centralising here removes that class of bug.
 *
 * <p>Canonical storage format: {@code 01XXXXXXXXX} (11 digits, leading 0,
 * no country code, no spaces, no {@code +}).
 */
public final class PhoneUtils {

    private PhoneUtils() { /* static-only */ }

    /**
     * Normalize a Bangladeshi phone number to the canonical 11-digit
     * {@code 01XXXXXXXXX} format stored in the database.
     *
     * <p>Accepted inputs:
     * <ul>
     *   <li>{@code 01XXXXXXXXX} (11 digits) — already canonical, returned as-is</li>
     *   <li>{@code 8801XXXXXXXXX} (13 digits) — country code stripped</li>
     *   <li>{@code +8801XXXXXXXXX} / {@code +1XXXXXXXXX} / spaces/dashes — cleaned</li>
     *   <li>{@code 1XXXXXXXXX} (10 digits, no leading 0) — leading 0 prepended</li>
     * </ul>
     *
     * <p>Returns {@code null} if {@code phone} is {@code null}. Returns the
     * trimmed digit-only input unchanged if it does not match any of the
     * above patterns (the caller is expected to validate separately).
     */
    public static String normalizeBd(String phone) {
        if (phone == null) return null;
        String cleaned = phone.trim().replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) return cleaned;

        // 8801XXXXXXXXX (13 digits) -> strip the 88 country code
        if (cleaned.startsWith("880") && cleaned.length() == 13) {
            return cleaned.substring(2);
        }
        // 01XXXXXXXXX (11 digits) -> already canonical
        if (cleaned.startsWith("01") && cleaned.length() == 11) {
            return cleaned;
        }
        // 1XXXXXXXXX (10 digits) -> prepend the leading 0 the user omitted
        if (cleaned.length() == 10) {
            return "0" + cleaned;
        }
        // Anything else: return the cleaned digits and let validation catch it
        return cleaned;
    }

    /**
     * Normalize a phone number to the {@code 8801XXXXXXXXX} form that the
     * BulkSMSBD gateway expects for outgoing SMS. Input is assumed to be
     * already in (or near) the canonical 11-digit format — call
     * {@link #normalizeBd(String)} first if you are unsure.
     */
    public static String toE164Bd(String phone) {
        if (phone == null) return null;
        String cleaned = phone.trim().replaceAll("[^0-9]", "");
        if (cleaned.startsWith("01") && cleaned.length() == 11) {
            return "88" + cleaned;
        }
        if (cleaned.startsWith("880") && cleaned.length() == 13) {
            return cleaned;
        }
        return cleaned;
    }
}
