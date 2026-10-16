package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OTP-first registration payload. Sent as multipart/form-data along with the
 * "idCard" file. The student/teacher row is NOT created until the matching
 * OTP is verified — see {@code PhoneVerificationService.verifyOtp}.
 *
 * Fields are role-discriminated: {@code rollNumber} is required when
 * {@code role=STUDENT}, {@code teacherId} when {@code role=TEACHER}.
 * Validation is enforced again at service time so we don't waste an OTP on
 * bad input.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneVerificationInitRequest {

    @NotNull(message = "রোল নির্বাচন করুন")
    private UserRole role;

    @NotBlank(message = "নাম দিন")
    @Size(min = 2, max = 100, message = "নাম ২ থেকে ১০০ অক্ষরের মধ্যে হতে হবে")
    private String name;

    @NotBlank(message = "ফোন নম্বর দিন")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "সঠিক ফোন নম্বর দিন")
    private String phone;

    @Size(min = 6, max = 128, message = "পাসওয়ার্ড ৬ থেকে ১২৮ অক্ষরের মধ্যে হতে হবে")
    private String password;

    /** Google ID token for Google Sign-In registrations. পাসওয়ার্ড দিন when this is absent. */
    private String googleIdToken;

    // ---- Student-only fields (ignored when role=TEACHER) ----
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
             message = "রোল নম্বর ২-৫০ অক্ষরের হতে হবে")
    private String rollNumber;

    @Pattern(regexp = "^([0-9]{1,2}|[0-9]{4}(-[0-9]{2,4})?)$",
             message = "সেশন সঠিকভাবে দিন (যেমন: 16, 2020, 2021-22, অথবা 2020-2024)")
    private String session;

    // ---- Teacher-only fields (ignored when role=STUDENT) ----
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
             message = "শিক্ষক আইডি ২-৫০ অক্ষরের হতে হবে")
    private String teacherId;

    @Size(max = 100, message = "পদবী ১০০ অক্ষরের মধ্যে হতে হবে")
    private String designation;

    // ---- Shared ----
    @NotBlank(message = "বিভাগ দিন")
    @Size(min = 2, max = 100, message = "বিভাগ ২ থেকে ১০০ অক্ষরের মধ্যে হতে হবে")
    private String department;
}
