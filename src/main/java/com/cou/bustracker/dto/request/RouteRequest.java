package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create/update payload for one bus route.
 *
 * <p>Both language forms are required: the row serves every locale, and the
 * server never picks a language on the client's behalf.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {

    @NotBlank(message = "Bengali route name is required")
    @Size(max = 255, message = "Bengali route name must be at most 255 characters")
    private String nameBn;

    @NotBlank(message = "English route name is required")
    @Size(max = 255, message = "English route name must be at most 255 characters")
    private String nameEn;
}
