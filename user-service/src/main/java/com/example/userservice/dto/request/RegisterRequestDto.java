package com.example.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min=6, max=100, message = "Пароль должен быть от 6 до 100 символов")
    private String password;

    @NotBlank(message = "Имя обязательно")
    private String firstName;

    @NotBlank(message= "Фамилия обязательна")
    private String lastName;

    @Pattern(regexp = "^\\\\+?[0-9]{10,15}$", message = "Некорректный номер телефона")
    private String phone;

    @NotBlank(message = "Необходимо указать должность")
    private String position;

    @NotBlank(message = "Указать департамент")
    private String department;
}
