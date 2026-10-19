package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One selectable option returned by the academic reference endpoints.
 *
 * <p>Shared by units (departments/offices) and sessions. The client submits
 * {@code id} back as the FK; {@code labelBn}/{@code labelEn} are both
 * returned so the app renders whichever matches its locale — the server
 * never picks a language.
 *
 * <p>{@code value} is the canonical string older app versions stored
 * verbatim (the Bengali unit name, or the English session range). It is
 * returned for display/legacy purposes only; the id is what the backend
 * persists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicOptionResponse {

    private Long id;

    /** Canonical stored string, kept for display and backward compatibility. */
    private String value;

    private String labelBn;

    private String labelEn;

    /** {@code DEPARTMENT} or {@code OFFICE}; null for sessions. */
    private String type;

    private Integer sortOrder;

    /**
     * Always {@code true} on the public registration pickers (they only list
     * active rows). The admin listings include retired rows, so this carries
     * the row's actual state there.
     */
    private Boolean active;
}
