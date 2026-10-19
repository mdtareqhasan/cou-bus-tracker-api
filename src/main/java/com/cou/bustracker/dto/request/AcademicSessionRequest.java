package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create/update payload for one academic session (e.g. {@code 2020-2021}).
 *
 * <p>Both language forms are required: the row serves every locale, and the
 * server never picks a language on the client's behalf. {@code sortOrder}
 * drives the registration picker's order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicSessionRequest {

    /** Bengali digits, e.g. '২০২০-২০২১'. */
    @NotBlank(message = "Bengali name is required")
    @Size(max = 20, message = "Bengali name must be at most 20 characters")
    private String nameBn;

    /** English range, e.g. '2020-2021' — the canonical stored value. */
    @NotBlank(message = "English name is required")
    @Size(max = 20, message = "English name must be at most 20 characters")
    private String nameEn;

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;
}
