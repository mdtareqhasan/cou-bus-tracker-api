package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAdminRequest {

    @NotBlank(message = "নাম দিন")
    @Size(max = 100, message = "নাম ১০০ অক্ষরের মধ্যে হতে হবে")
    private String name;

    @NotBlank(message = "ইমেইল দিন")
    @Email(message = "সঠিক ইমেইল দিন")
    @Size(max = 100, message = "ইমেইল ১০০ অক্ষরের মধ্যে হতে হবে")
    private String email;

    @NotBlank(message = "পাসওয়ার্ড দিন")
    @Size(min = 6, max = 100, message = "পাসওয়ার্ড ৬ থেকে ১০০ অক্ষরের মধ্যে হতে হবে")
    private String password;
}
