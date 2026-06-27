package org.example.goldenheartrestaurant.modules.menu.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.menu.dto.response.PopularDishResponse;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.example.goldenheartrestaurant.modules.menu.repository.PopularDishProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PublicMenuService {

    private static final int BESTSELLER_PER_CATEGORY = 2;

    /**
     * Từ khoá trong tên danh mục dùng để loại đồ uống đóng chai khỏi homepage.
     * Dùng tên category vì production_station trong DB có thể không được set đúng (default KITCHEN).
     */
    private static final Set<String> DRINK_CATEGORY_KEYWORDS = Set.of(
            "nước uống", "đồ uống", "đóng chai", "nước đóng chai", "drink", "beverage"
    );

    private final MenuItemRepository menuItemRepository;

    /**
     * Trả về danh sách món ăn phổ biến cho homepage.
     *
     * <p>Quy tắc bestseller: top {@value BESTSELLER_PER_CATEGORY} món gọi nhiều nhất
     * trong mỗi danh mục được gắn cờ bestSeller = true.
     * Vì query đã ORDER BY totalOrderCount DESC, ta chỉ cần đếm
     * số lần mỗi categoryId xuất hiện và gắn cờ cho 2 lần đầu tiên.
     */
    @Transactional(readOnly = true)
    public List<PopularDishResponse> getPopularDishes() {
        List<PopularDishProjection> rows = menuItemRepository.findPopularDishesForHomepage()
                .stream()
                // Homepage không hiển thị đồ uống đóng chai.
                // Filter theo tên category vì production_station trong DB có thể là KITCHEN (default).
                // POS dùng API riêng (/api/v1/menu-items) — không bị ảnh hưởng.
                .filter(row -> !isDrinkCategory(row.getCategoryName()))
                .toList();

        // Đếm số món đã được gắn bestSeller theo từng categoryId
        Map<Integer, Integer> bestSellerCount = new HashMap<>();

        List<PopularDishResponse> result = new ArrayList<>(rows.size());
        for (PopularDishProjection row : rows) {
            int count = bestSellerCount.getOrDefault(row.getCategoryId(), 0);
            boolean isBestSeller = row.getTotalOrderCount() > 0 && count < BESTSELLER_PER_CATEGORY;

            if (isBestSeller) {
                bestSellerCount.put(row.getCategoryId(), count + 1);
            }

            result.add(PopularDishResponse.builder()
                    .id(row.getId())
                    .name(row.getName())
                    .imageUrl(row.getImageUrl())
                    .description(row.getDescription())
                    .price(row.getPrice())
                    .categoryId(row.getCategoryId())
                    .categoryName(row.getCategoryName())
                    .totalOrderCount(row.getTotalOrderCount())
                    .bestSeller(isBestSeller)
                    .build());
        }
        return result;
    }

    private boolean isDrinkCategory(String categoryName) {
        if (categoryName == null) return false;
        String lower = categoryName.toLowerCase();
        return DRINK_CATEGORY_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
