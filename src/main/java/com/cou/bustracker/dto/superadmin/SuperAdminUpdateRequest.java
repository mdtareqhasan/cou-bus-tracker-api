package com.cou.bustracker.dto.superadmin;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminUpdateRequest {

    private String fullName;

    private Boolean isActive;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}