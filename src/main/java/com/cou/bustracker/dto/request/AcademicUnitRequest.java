package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.AcademicUnit.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create/update payload for one academic unit (department or office).
 *
 * <p>Both language forms are required: the row serves every locale, and the
 * server never picks a language on the client's behalf. {@code sortOrder}
 * drives the registration picker's order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicUnitRequest {

    @NotNull(message = "Unit type is required")
    private UnitType unitType;

    @NotBlank(message = "Bengali name is required")
    @Size(max = 200, message = "Bengali name must be at most 200 characters")
    private String nameBn;

    @NotBlank(message = "English name is required")
    @Size(max = 200, message = "English name must be at most 200 characters")
    private String nameEn;

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;
}
