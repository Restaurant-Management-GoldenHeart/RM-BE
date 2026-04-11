package org.example.goldenheartrestaurant.modules.customer.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.customer.dto.request.CreateCustomerRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.request.UpdateCustomerRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerResponse;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
/**
 * Service xử lý nghiệp vụ khách hàng.
 *
 * Các rule chính nằm ở đây:
 * - tìm kiếm phân trang
 * - uniqueness cho email / phone / customerCode
 * - soft delete thông qua repository / entity
 */
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(String keyword, int page, int size) {
        Page<Customer> customers = customerRepository.search(normalizeKeyword(keyword), PageRequest.of(page, size));

        return PageResponse.<CustomerResponse>builder()
                .content(customers.getContent().stream().map(this::toCustomerResponse).toList())
                .page(customers.getNumber())
                .size(customers.getSize())
                .totalElements(customers.getTotalElements())
                .totalPages(customers.getTotalPages())
                .last(customers.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Integer customerId) {
        return toCustomerResponse(getCustomerOrThrow(customerId));
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        validateCustomerUniquenessForCreate(request.email(), request.phone(), request.customerCode());

        Customer customer = Customer.builder()
                .customerCode(request.customerCode())
                .name(request.name())
                .phone(request.phone())
                .activePhone(request.phone())
                .email(request.email())
                .activeEmail(request.email())
                .loyaltyPoints(0)
                .address(request.address())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .note(request.note())
                .build();

        // Khách mới luôn bắt đầu từ 0 loyalty point.
        // Sau này order/billing flow có thể tăng điểm.
        return toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse updateCustomer(Integer customerId, UpdateCustomerRequest request) {
        Customer customer = getCustomerOrThrow(customerId);

        if (StringUtils.hasText(request.name())) {
            customer.setName(request.name());
        }
        if (request.email() != null) {
            validateEmailForUpdate(request.email(), customerId);
            customer.setEmail(request.email());
            customer.setActiveEmail(request.email());
        }
        if (request.phone() != null) {
            validatePhoneForUpdate(request.phone(), customerId);
            customer.setPhone(request.phone());
            customer.setActivePhone(request.phone());
        }
        if (request.customerCode() != null) {
            validateCustomerCodeForUpdate(request.customerCode(), customerId);
            customer.setCustomerCode(request.customerCode());
        }
        if (request.address() != null) {
            customer.setAddress(request.address());
        }
        if (request.dateOfBirth() != null) {
            customer.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            customer.setGender(request.gender());
        }
        if (request.note() != null) {
            customer.setNote(request.note());
        }

        return toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(Integer customerId) {
        customerRepository.delete(getCustomerOrThrow(customerId));
    }

    private Customer getCustomerOrThrow(Integer customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    private void validateCustomerUniquenessForCreate(String email, String phone, String customerCode) {
        if (StringUtils.hasText(email) && customerRepository.existsByActiveEmailIgnoreCase(email)) {
            throw new ConflictException("Email already exists");
        }
        if (StringUtils.hasText(phone) && customerRepository.existsByActivePhone(phone)) {
            throw new ConflictException("Phone already exists");
        }
        if (StringUtils.hasText(customerCode) && customerRepository.existsByCustomerCodeIgnoreCase(customerCode)) {
            throw new ConflictException("Customer code already exists");
        }
    }

    private void validateEmailForUpdate(String email, Integer customerId) {
        if (StringUtils.hasText(email) && customerRepository.existsByActiveEmailIgnoreCaseAndIdNot(email, customerId)) {
            throw new ConflictException("Email already exists");
        }
    }

    private void validatePhoneForUpdate(String phone, Integer customerId) {
        if (StringUtils.hasText(phone) && customerRepository.existsByActivePhoneAndIdNot(phone, customerId)) {
            throw new ConflictException("Phone already exists");
        }
    }

    private void validateCustomerCodeForUpdate(String customerCode, Integer customerId) {
        if (StringUtils.hasText(customerCode) && customerRepository.existsByCustomerCodeIgnoreCaseAndIdNot(customerCode, customerId)) {
            throw new ConflictException("Customer code already exists");
        }
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getLoyaltyPoints(),
                customer.getAddress(),
                customer.getDateOfBirth(),
                customer.getGender(),
                customer.getNote(),
                customer.getLastVisitAt(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
