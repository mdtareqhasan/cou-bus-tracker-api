package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegisterRequest {

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

    @NotBlank(message = "ছাত্র আইডি দিন")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$", message = "ছাত্র আইডি ২-৫০ অক্ষরের হতে হবে")
    private String studentId;

    @NotBlank(message = "বিভাগ দিন")
    @Size(min = 2, max = 100, message = "বিভাগ ২ থেকে ১০০ অক্ষরের মধ্যে হতে হবে")
    private String department;

    @NotBlank(message = "ব্যাচ দিন")
    @Pattern(
            regexp = "^([0-9]{1,2}|[0-9]{4}(-[0-9]{4})?)$",
            message = "ব্যাচ সঠিকভাবে দিন (যেমন: 16, 2020, অথবা 2020-2024)")
    private String varsityBatch;
}
