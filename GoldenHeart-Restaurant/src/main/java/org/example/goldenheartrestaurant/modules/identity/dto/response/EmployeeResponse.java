package org.example.goldenheartrestaurant.modules.identity.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Integer id,
        String username,
        String status,
        Integer roleId,
        String roleName,
        String fullName,
        String employeeCode,
        String email,
        String phone,
        Integer branchId,
        String branchName,
        LocalDate dateOfBirth,
        String gender,
        LocalDate hireDate,
        BigDecimal salary,
        String address,
        String internalNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
