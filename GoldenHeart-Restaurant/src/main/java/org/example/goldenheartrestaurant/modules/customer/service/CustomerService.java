package org.example.goldenheartrestaurant.modules.customer.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.BadRequestException;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.customer.dto.request.CreateCustomerRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.request.QuickCreateCustomerRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.request.UpdateCustomerRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerLookupResponse;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerLoyaltyTransactionResponse;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerResponse;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerTier;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Service xu ly nghiep vu customer.
 *
 * Ngoai CRUD CRM co san, service nay bo sung them:
 * - lookup nhanh cho POS
 * - tao nhanh customer moi ngay luc thanh toan
 * - tra ve thong tin tier hien tai
 * - tra lich su loyalty transaction
 */
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerLoyaltyService customerLoyaltyService;

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
    public List<CustomerLookupResponse> lookupCustomers(String keyword, int size) {
        if (!StringUtils.hasText(keyword)) {
            throw new BadRequestException("Keyword is required for customer lookup");
        }

        Page<Customer> customers = customerRepository.search(keyword.trim(), PageRequest.of(0, size));
        return customers.getContent().stream().map(this::toLookupResponse).toList();
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

        return toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerLookupResponse quickCreateCustomer(QuickCreateCustomerRequest request) {
        if (!StringUtils.hasText(request.phone()) && !StringUtils.hasText(request.email())) {
            throw new BadRequestException("Phone or email is required for quick customer creation");
        }

        validateCustomerUniquenessForCreate(request.email(), request.phone(), null);

        Customer customer = Customer.builder()
                .name(request.name().trim())
                .phone(request.phone())
                .activePhone(request.phone())
                .email(request.email())
                .activeEmail(request.email())
                .loyaltyPoints(0)
                .note(request.note())
                .build();

        return toLookupResponse(customerRepository.save(customer));
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

    @Transactional(readOnly = true)
    public PageResponse<CustomerLoyaltyTransactionResponse> getCustomerTransactions(Integer customerId, int page, int size) {
        getCustomerOrThrow(customerId);
        return customerLoyaltyService.getCustomerTransactions(customerId, page, size);
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
        CustomerTier tier = customerLoyaltyService.resolveTier(customer.getLoyaltyPoints());
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
                customer.getUpdatedAt(),
                tier != null ? tier.getId() : null,
                tier != null ? tier.getCode() : null,
                tier != null ? tier.getName() : null,
                tier != null ? tier.getDiscountRate() : null
        );
    }

    private CustomerLookupResponse toLookupResponse(Customer customer) {
        CustomerTier tier = customerLoyaltyService.resolveTier(customer.getLoyaltyPoints());
        return new CustomerLookupResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getLoyaltyPoints(),
                tier != null ? tier.getId() : null,
                tier != null ? tier.getCode() : null,
                tier != null ? tier.getName() : null,
                tier != null ? tier.getDiscountRate() : null,
                customer.getLastVisitAt()
        );
    }
}
