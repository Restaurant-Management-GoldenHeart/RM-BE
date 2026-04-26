package org.example.goldenheartrestaurant.modules.customer.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.modules.customer.dto.request.CreateCustomerTierRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.request.UpdateCustomerTierRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerTierResponse;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerTier;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Service quan ly bang hang khach hang.
 *
 * Tier la du lieu cau hinh nen cho phep ADMIN quan ly,
 * con cac role khac chi can doc de hien thi.
 */
public class CustomerTierService {

    private final CustomerTierRepository customerTierRepository;

    @Transactional(readOnly = true)
    public List<CustomerTierResponse> getCustomerTiers(boolean activeOnly) {
        List<CustomerTier> tiers = activeOnly
                ? customerTierRepository.findAllByActiveTrueOrderByMinPointsAsc()
                : customerTierRepository.findAllByOrderByMinPointsAsc();

        return tiers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerTierResponse getCustomerTierById(Integer tierId) {
        return toResponse(getTierOrThrow(tierId));
    }

    @Transactional
    public CustomerTierResponse createCustomerTier(CreateCustomerTierRequest request) {
        validateUniqueness(request.code(), request.name(), request.minPoints(), null);

        CustomerTier tier = CustomerTier.builder()
                .code(request.code().trim().toUpperCase())
                .name(request.name().trim())
                .minPoints(request.minPoints())
                .discountRate(request.discountRate())
                .active(request.active())
                .note(request.note())
                .build();

        return toResponse(customerTierRepository.save(tier));
    }

    @Transactional
    public CustomerTierResponse updateCustomerTier(Integer tierId, UpdateCustomerTierRequest request) {
        CustomerTier tier = getTierOrThrow(tierId);
        validateUniqueness(request.code(), request.name(), request.minPoints(), tierId);

        tier.setCode(request.code().trim().toUpperCase());
        tier.setName(request.name().trim());
        tier.setMinPoints(request.minPoints());
        tier.setDiscountRate(request.discountRate());
        tier.setActive(request.active());
        tier.setNote(request.note());

        return toResponse(customerTierRepository.save(tier));
    }

    @Transactional
    public void deactivateCustomerTier(Integer tierId) {
        CustomerTier tier = getTierOrThrow(tierId);
        tier.setActive(false);
        customerTierRepository.save(tier);
    }

    private CustomerTier getTierOrThrow(Integer tierId) {
        return customerTierRepository.findById(tierId)
                .orElseThrow(() -> new NotFoundException("Customer tier not found"));
    }

    private void validateUniqueness(String code, String name, Integer minPoints, Integer tierId) {
        boolean codeExists = tierId == null
                ? customerTierRepository.existsByCodeIgnoreCase(code)
                : customerTierRepository.existsByCodeIgnoreCaseAndIdNot(code, tierId);
        if (codeExists) {
            throw new ConflictException("Customer tier code already exists");
        }

        boolean nameExists = tierId == null
                ? customerTierRepository.existsByNameIgnoreCase(name)
                : customerTierRepository.existsByNameIgnoreCaseAndIdNot(name, tierId);
        if (nameExists) {
            throw new ConflictException("Customer tier name already exists");
        }

        boolean minPointsExists = tierId == null
                ? customerTierRepository.existsByMinPoints(minPoints)
                : customerTierRepository.existsByMinPointsAndIdNot(minPoints, tierId);
        if (minPointsExists) {
            throw new ConflictException("Customer tier min points already exists");
        }
    }

    private CustomerTierResponse toResponse(CustomerTier tier) {
        return new CustomerTierResponse(
                tier.getId(),
                tier.getCode(),
                tier.getName(),
                tier.getMinPoints(),
                tier.getDiscountRate(),
                tier.getActive(),
                tier.getNote(),
                tier.getCreatedAt(),
                tier.getUpdatedAt()
        );
    }
}
