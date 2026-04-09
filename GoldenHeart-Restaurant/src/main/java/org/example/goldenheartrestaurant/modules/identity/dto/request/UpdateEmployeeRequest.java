package org.example.goldenheartrestaurant.modules.identity.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateEmployeeRequest(
        @Size(max = 100)
        String fullName,

        @Email
        @Size(max = 100)
        String email,

        @Pattern(regexp = "^[0-9+\\-() ]{8,20}$", message = "Phone number is invalid")
        String phone,

        @Size(max = 30)
        String employeeCode,

        Integer branchId,

        @Past
        LocalDate dateOfBirth,

        @Size(max = 10)
        String gender,

        LocalDate hireDate,

        @DecimalMin(value = "0.00")
        BigDecimal salary,

        @Size(max = 255)
        String address,

        @Size(max = 1000)
        String internalNotes,

        Integer roleId,

        String status
) {
}
