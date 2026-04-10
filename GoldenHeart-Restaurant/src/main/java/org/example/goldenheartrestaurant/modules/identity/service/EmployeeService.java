package org.example.goldenheartrestaurant.modules.identity.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.dto.request.CreateEmployeeRequest;
import org.example.goldenheartrestaurant.modules.identity.dto.request.UpdateEmployeeRequest;
import org.example.goldenheartrestaurant.modules.identity.dto.request.UpdateOwnEmployeeProfileRequest;
import org.example.goldenheartrestaurant.modules.identity.dto.response.EmployeeResponse;
import org.example.goldenheartrestaurant.modules.identity.dto.response.EmployeeSelfResponse;
import org.example.goldenheartrestaurant.modules.identity.entity.Role;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.entity.UserProfile;
import org.example.goldenheartrestaurant.modules.identity.entity.UserStatus;
import org.example.goldenheartrestaurant.modules.identity.repository.RoleRepository;
import org.example.goldenheartrestaurant.modules.identity.repository.UserProfileRepository;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
/**
 * Employee domain service.
 *
 * Important design choice: some authorization is duplicated here even though controllers already use
 * @PreAuthorize, because rules like "manager cannot assign role" are business constraints, not just
 * simple role membership checks.
 */
public class EmployeeService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getEmployees(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> employees = userRepository.searchEmployees(normalizeKeyword(keyword), pageable);

        return PageResponse.<EmployeeResponse>builder()
                .content(employees.getContent().stream().map(this::toEmployeeResponse).toList())
                .page(employees.getNumber())
                .size(employees.getSize())
                .totalElements(employees.getTotalElements())
                .totalPages(employees.getTotalPages())
                .last(employees.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Integer employeeId) {
        return toEmployeeResponse(getEmployeeOrThrow(employeeId));
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request, CustomUserDetails currentUser) {
        boolean isAdmin = hasRole(currentUser, ROLE_ADMIN);
        boolean isManager = hasRole(currentUser, ROLE_MANAGER);

        // Service-layer validation keeps permission-sensitive write rules close to the mutation itself.
        validateCreateRequest(request, isAdmin, isManager);

        Role role = resolveRoleForCreate(request.roleId(), isAdmin);
        Branch branch = resolveBranch(request.branchId());

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        UserProfile profile = UserProfile.builder()
                .user(user)
                .fullName(request.fullName())
                .employeeCode(request.employeeCode())
                .email(request.email())
                .activeEmail(request.email())
                .phone(request.phone())
                .activePhone(request.phone())
                .branch(branch)
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .hireDate(request.hireDate())
                .salary(request.salary())
                .address(request.address())
                .internalNotes(request.internalNotes())
                .build();

        user.setProfile(profile);

        return toEmployeeResponse(userRepository.save(user));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Integer employeeId, UpdateEmployeeRequest request, CustomUserDetails currentUser) {
        User employee = getEmployeeOrThrow(employeeId);
        boolean isAdmin = hasRole(currentUser, ROLE_ADMIN);
        boolean isManager = hasRole(currentUser, ROLE_MANAGER);

        if (!isAdmin && !isManager) {
            throw new ForbiddenException("You do not have permission to update employees");
        }

        if (!isAdmin && request.roleId() != null) {
            // Manager can create employees but cannot choose arbitrary roles.
            throw new ForbiddenException("Manager cannot change employee role");
        }

        UserProfile profile = employee.getProfile();

        if (StringUtils.hasText(request.fullName())) {
            profile.setFullName(request.fullName());
        }
        if (request.email() != null) {
            validateEmailForUpdate(request.email(), employeeId);
            profile.setEmail(request.email());
            profile.setActiveEmail(request.email());
        }
        if (request.phone() != null) {
            validatePhoneForUpdate(request.phone(), employeeId);
            profile.setPhone(request.phone());
            profile.setActivePhone(request.phone());
        }
        if (request.employeeCode() != null) {
            validateEmployeeCodeForUpdate(request.employeeCode(), employeeId);
            profile.setEmployeeCode(request.employeeCode());
        }
        if (request.branchId() != null) {
            profile.setBranch(resolveBranch(request.branchId()));
        }
        if (request.dateOfBirth() != null) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }
        if (request.hireDate() != null) {
            profile.setHireDate(request.hireDate());
        }
        if (request.salary() != null) {
            profile.setSalary(request.salary());
        }
        if (request.address() != null) {
            profile.setAddress(request.address());
        }
        if (request.internalNotes() != null) {
            profile.setInternalNotes(request.internalNotes());
        }
        if (request.status() != null) {
            employee.setStatus(UserStatus.valueOf(request.status().trim().toUpperCase()));
        }
        if (isAdmin && request.roleId() != null && !request.roleId().equals(employee.getRole().getId())) {
            employee.setRole(resolveRoleById(request.roleId()));
        }

        return toEmployeeResponse(userRepository.save(employee));
    }

    @Transactional
    public void deleteEmployee(Integer employeeId, CustomUserDetails currentUser) {
        if (!hasRole(currentUser, ROLE_ADMIN)) {
            throw new ForbiddenException("Only admin can delete employees");
        }
        if (employeeId.equals(currentUser.getUserId())) {
            // Prevent accidental self-delete of the current authenticated account.
            throw new ForbiddenException("You cannot delete your own account");
        }

        User employee = getEmployeeOrThrow(employeeId);
        userProfileRepository.softDeleteByUserId(employeeId);
        userRepository.delete(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeSelfResponse getMyProfile(Integer currentUserId) {
        User employee = getEmployeeOrThrow(currentUserId);
        return toEmployeeSelfResponse(employee);
    }

    @Transactional
    public EmployeeSelfResponse updateMyProfile(Integer currentUserId, UpdateOwnEmployeeProfileRequest request) {
        User employee = getEmployeeOrThrow(currentUserId);
        UserProfile profile = employee.getProfile();

        if (StringUtils.hasText(request.fullName())) {
            profile.setFullName(request.fullName());
        }
        if (request.email() != null) {
            validateEmailForUpdate(request.email(), currentUserId);
            profile.setEmail(request.email());
            profile.setActiveEmail(request.email());
        }
        if (request.phone() != null) {
            validatePhoneForUpdate(request.phone(), currentUserId);
            profile.setPhone(request.phone());
            profile.setActivePhone(request.phone());
        }
        if (request.address() != null) {
            profile.setAddress(request.address());
        }
        if (request.dateOfBirth() != null) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }

        return toEmployeeSelfResponse(userRepository.save(employee));
    }

    private void validateCreateRequest(CreateEmployeeRequest request, boolean isAdmin, boolean isManager) {
        if (!isAdmin && !isManager) {
            throw new ForbiddenException("You do not have permission to create employees");
        }
        if (!isAdmin && request.roleId() != null) {
            // Managers are limited to creating operational STAFF accounts.
            throw new ForbiddenException("Manager cannot assign role when creating employee");
        }
        if (isAdmin && request.roleId() == null) {
            throw new ConflictException("Role is required for admin when creating employee");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Username already exists");
        }
        validateEmailForCreate(request.email());
        validatePhoneForCreate(request.phone());
        validateEmployeeCodeForCreate(request.employeeCode());
    }

    private Role resolveRoleForCreate(Integer roleId, boolean isAdmin) {
        if (isAdmin) {
            return resolveRoleById(roleId);
        }

        // Manager-created accounts default to STAFF to avoid privilege escalation by request body tampering.
        return roleRepository.findByNameIgnoreCase(ROLE_STAFF)
                .orElseThrow(() -> new NotFoundException("Default STAFF role not found"));
    }

    private Role resolveRoleById(Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found"));
    }

    private Branch resolveBranch(Integer branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private User getEmployeeOrThrow(Integer employeeId) {
        return userRepository.findEmployeeDetailById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private void validateEmailForCreate(String email) {
        if (StringUtils.hasText(email) && userProfileRepository.existsByActiveEmailIgnoreCase(email)) {
            throw new ConflictException("Email already exists");
        }
    }

    private void validatePhoneForCreate(String phone) {
        if (StringUtils.hasText(phone) && userProfileRepository.existsByActivePhone(phone)) {
            throw new ConflictException("Phone already exists");
        }
    }

    private void validateEmployeeCodeForCreate(String employeeCode) {
        if (StringUtils.hasText(employeeCode) && userProfileRepository.existsByEmployeeCodeIgnoreCase(employeeCode)) {
            throw new ConflictException("Employee code already exists");
        }
    }

    private void validateEmailForUpdate(String email, Integer userId) {
        if (StringUtils.hasText(email) && userProfileRepository.existsByActiveEmailIgnoreCaseAndUserIdNot(email, userId)) {
            throw new ConflictException("Email already exists");
        }
    }

    private void validatePhoneForUpdate(String phone, Integer userId) {
        if (StringUtils.hasText(phone) && userProfileRepository.existsByActivePhoneAndUserIdNot(phone, userId)) {
            throw new ConflictException("Phone already exists");
        }
    }

    private void validateEmployeeCodeForUpdate(String employeeCode, Integer userId) {
        if (StringUtils.hasText(employeeCode) && userProfileRepository.existsByEmployeeCodeIgnoreCaseAndUserIdNot(employeeCode, userId)) {
            throw new ConflictException("Employee code already exists");
        }
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private boolean hasRole(CustomUserDetails user, String role) {
        return role.equalsIgnoreCase(user.getRoleName());
    }

    private EmployeeResponse toEmployeeResponse(User user) {
        UserProfile profile = user.getProfile();
        Branch branch = profile.getBranch();

        return new EmployeeResponse(
                user.getId(),
                user.getUsername(),
                user.getStatus().name(),
                user.getRole().getId(),
                user.getRole().getName(),
                profile.getFullName(),
                profile.getEmployeeCode(),
                profile.getEmail(),
                profile.getPhone(),
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null,
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getHireDate(),
                profile.getSalary(),
                profile.getAddress(),
                profile.getInternalNotes(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private EmployeeSelfResponse toEmployeeSelfResponse(User user) {
        UserProfile profile = user.getProfile();
        Branch branch = profile.getBranch();

        return new EmployeeSelfResponse(
                user.getId(),
                user.getUsername(),
                user.getStatus().name(),
                user.getRole().getName(),
                profile.getFullName(),
                profile.getEmployeeCode(),
                profile.getEmail(),
                profile.getPhone(),
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null,
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getHireDate(),
                profile.getAddress(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
