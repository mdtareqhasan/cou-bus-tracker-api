package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationInitRequest {

    @NotNull(message = "রোল নির্বাচন করুন")
    private UserRole role;

    @NotBlank(message = "নাম দিন")
    @Size(min = 2, max = 100, message = "নাম ২ থেকে ১০০ অক্ষরের মধ্যে হতে হবে")
    private String name;

    @Email(message = "সঠিক ইমেইল দিন")
    @NotBlank(message = "ইমেইল দিন")
    private String email;

    @Size(min = 6, max = 128, message = "পাসওয়ার্ড ৬ থেকে ১২৮ অক্ষরের মধ্যে হতে হবে")
    private String password;

    /** Google ID token for Google Sign-In registrations. পাসওয়ার্ড দিন when this is absent. */
    private String googleIdToken;

    // ---- Student-only fields ----
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
            message = "রোল নম্বর ২-৫০ অক্ষরের হতে হবে")
    private String rollNumber;

    @Pattern(regexp = "^([0-9]{1,2}|[0-9]{4}(-[0-9]{2,4})?)$",
            message = "সেশন সঠিকভাবে দিন (যেমন: 16, 2020, 2021-22, অথবা 2020-2024)")
    private String session;

    // ---- Employee-only fields ----
    /** Accepted as employeeId in the API, mapped to existing teachers.teacher_id. */
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
            message = "কর্মচারী আইডি ২-৫০ অক্ষরের হতে হবে")
    private String employeeId;

    @Size(max = 100, message = "পদবী ১০০ অক্ষরের মধ্যে হতে হবে")
    private String designation;

    @NotBlank(message = "বিভাগ দিন")
    @Size(min = 2, max = 100, message = "বিভাগ ২ থেকে ১০০ অক্ষরের মধ্যে হতে হবে")
    private String department;
}
