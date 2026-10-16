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
public class TeacherRegisterRequest {

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

    @NotBlank(message = "শিক্ষক আইডি দিন")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$", message = "শিক্ষক আইডি ২-৫০ অক্ষরের হতে হবে")
    private String teacherId;

    @Size(max = 100, message = "পদবী ১০০ অক্ষরের মধ্যে হতে হবে")
    private String designation;

    @NotBlank(message = "বিভাগ দিন")
    @Size(min = 2, max = 100, message = "বিভাগ ২ থেকে ১০০ অক্ষরের মধ্যে হতে হবে")
    private String department;


}
