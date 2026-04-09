package org.example.goldenheartrestaurant.modules.identity.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeSelfResponse(
        Integer id,
        String username,
        String status,
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
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
