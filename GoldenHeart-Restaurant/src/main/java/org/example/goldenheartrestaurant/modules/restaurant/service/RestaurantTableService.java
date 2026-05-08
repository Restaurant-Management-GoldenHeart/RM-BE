package org.example.goldenheartrestaurant.modules.restaurant.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.order.repository.OrderRepository;
import org.example.goldenheartrestaurant.modules.order.service.OrderManagementService;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.CreateRestaurantTableRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateTableStatusRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateRestaurantTableRequest;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.entity.DiningArea;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.RestaurantTableResponse;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTable;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.DiningAreaRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service quản lý dữ liệu bàn ăn và trạng thái vận hành của bàn.
 *
 * Phần CRUD bàn tưởng đơn giản nhưng thực tế bị ràng buộc chặt với luồng order:
 * - không được sửa/xóa bàn đang có active order
 * - không được tự ý set OCCUPIED bằng tay vì trạng thái đó thuộc quyền của order workflow
 * - khi bàn đã gộp, bàn thành viên không được thao tác như bàn độc lập
 *
 * Lớp này tập trung xử lý:
 * - tạo, sửa, xóa bàn
 * - đổi trạng thái thủ công cho các trạng thái vận hành hợp lệ
 * - dựng response có thông tin nhóm bàn để frontend dễ hiển thị
 */
@Service
@RequiredArgsConstructor
public class RestaurantTableService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";
    private static final String ROLE_KITCHEN = "KITCHEN";
    private static final Comparator<RestaurantTable> TABLE_GROUP_COMPARATOR = Comparator
            .comparing(RestaurantTable::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(RestaurantTable::getTableNumber, String.CASE_INSENSITIVE_ORDER);

    private final RestaurantTableRepository restaurantTableRepository;
    private final BranchRepository branchRepository;
    private final DiningAreaRepository diningAreaRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderManagementService orderManagementService;

    @Transactional(readOnly = true)
    public List<RestaurantTableResponse> getTables(Integer branchId,
                                                   String status,
                                                   String keyword,
                                                   CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        Integer scopedBranchId = resolveAccessibleBranchId(branchId, currentUser);
        RestaurantTableStatus statusFilter = parseStatus(status);
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;

        List<RestaurantTable> baseTables = scopedBranchId != null
                ? restaurantTableRepository.findAllForListingBaseByBranchId(scopedBranchId)
                : restaurantTableRepository.findAllForListingBase();

        List<RestaurantTable> tables = baseTables
                .stream()
                .filter(table -> statusFilter == null || table.getStatus() == statusFilter)
                .filter(table -> matchesListingKeyword(table, normalizedKeyword))
                .toList();
        // Tải trước toàn bộ nhóm gộp liên quan để tránh mỗi bàn lại phải truy vấn riêng.
        Map<Integer, List<RestaurantTable>> mergedGroupMap = loadMergedGroupMap(tables);

        return tables
                .stream()
                .map(table -> toResponse(table, mergedGroupMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantTableResponse getTableById(Integer tableId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        RestaurantTable table = getTableEntity(tableId);
        Integer scopedBranchId = resolveAccessibleBranchId(table.getBranch().getId(), currentUser);
        if (!table.getBranch().getId().equals(scopedBranchId)) {
            throw new ForbiddenException("You do not have permission to view this table");
        }

        Map<Integer, List<RestaurantTable>> mergedGroupMap = loadMergedGroupMap(List.of(table));
        return toResponse(table, mergedGroupMap);
    }

    @Transactional
    public RestaurantTableResponse createTable(CreateRestaurantTableRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        Integer scopedBranchId = resolveAccessibleBranchId(request.branchId(), currentUser);
        Branch branch = resolveBranch(scopedBranchId);
        DiningArea area = resolveArea(request.areaId(), scopedBranchId);
        String normalizedTableNumber = normalizeTableNumber(request.tableNumber());
        ensureTableNumberAvailable(scopedBranchId, normalizedTableNumber, null);

        RestaurantTable table = RestaurantTable.builder()
                .branch(branch)
                .area(area)
                .tableNumber(normalizedTableNumber)
                .capacity(request.capacity())
                .posX(request.posX())
                .posY(request.posY())
                .width(request.width())
                .height(request.height())
                .displayOrder(request.displayOrder())
                .status(RestaurantTableStatus.AVAILABLE)
                .build();

        return toResponse(restaurantTableRepository.save(table));
    }

    @Transactional
    public RestaurantTableResponse updateTable(Integer tableId,
                                               UpdateRestaurantTableRequest request,
                                               CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        RestaurantTable table = getTableEntity(tableId);
        Integer scopedBranchId = resolveAccessibleBranchId(request.branchId(), currentUser);
        boolean hasOrderHistory = orderRepository.existsByTable_Id(tableId);

        if (orderManagementService.findActiveOrderEntityByTableId(tableId).isPresent()) {
            throw new ConflictException("Cannot edit a table that still has an active order");
        }
        // Bàn đang là thành viên hoặc đang là bàn gốc của một nhóm gộp đều không được sửa cấu trúc.
        if (isMergedMember(table) || hasMergedMembers(table.getId())) {
            throw new ConflictException("Cannot edit a table while it is part of a merged-table group");
        }
        if (hasOrderHistory && !table.getBranch().getId().equals(scopedBranchId)) {
            throw new ConflictException("Cannot move a table to another branch after it already has order history");
        }

        Branch branch = resolveBranch(scopedBranchId);
        DiningArea area = resolveArea(request.areaId(), scopedBranchId);
        String normalizedTableNumber = normalizeTableNumber(request.tableNumber());
        ensureTableNumberAvailable(scopedBranchId, normalizedTableNumber, tableId);

        table.setBranch(branch);
        table.setArea(area);
        table.setTableNumber(normalizedTableNumber);
        table.setCapacity(request.capacity());
        table.setPosX(request.posX());
        table.setPosY(request.posY());
        table.setWidth(request.width());
        table.setHeight(request.height());
        table.setDisplayOrder(request.displayOrder());

        return toResponse(restaurantTableRepository.save(table));
    }

    @Transactional
    public void deleteTable(Integer tableId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN);

        RestaurantTable table = getTableEntity(tableId);
        Integer scopedBranchId = resolveAccessibleBranchId(table.getBranch().getId(), currentUser);
        if (!table.getBranch().getId().equals(scopedBranchId)) {
            throw new ForbiddenException("You do not have permission to delete this table");
        }
        if (orderManagementService.findActiveOrderEntityByTableId(tableId).isPresent()) {
            throw new ConflictException("Cannot delete a table that still has an active order");
        }
        if (isMergedMember(table) || hasMergedMembers(tableId)) {
            throw new ConflictException("Cannot delete a table while it is part of a merged-table group");
        }
        if (orderRepository.existsByTable_Id(tableId)) {
            throw new ConflictException("Cannot delete a table that already has order history");
        }

        restaurantTableRepository.delete(table);
    }

    @Transactional
    public RestaurantTableResponse updateTableStatus(Integer tableId,
                                                     UpdateTableStatusRequest request,
                                                     CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        RestaurantTable table = getTableEntity(tableId);
        Integer scopedBranchId = resolveAccessibleBranchId(table.getBranch().getId(), currentUser);
        if (!table.getBranch().getId().equals(scopedBranchId)) {
            throw new ForbiddenException("You do not have permission to update this table");
        }
        if (orderManagementService.findActiveOrderEntityByTableId(tableId).isPresent()) {
            throw new ConflictException("Cannot manually change a table that still has an active order");
        }
        // Bàn thành viên không được đổi trạng thái thủ công.
        // Mọi thay đổi của nó phải đi theo bàn gốc để cả nhóm luôn đồng bộ.
        if (isMergedMember(table)) {
            throw new ConflictException("Merged member tables are managed through the root table");
        }

        RestaurantTableStatus targetStatus = parseRequiredStatus(request.status());
        validateStatusTransition(table.getStatus(), targetStatus);

        if (table.getStatus() == RestaurantTableStatus.CLEANING
                && targetStatus == RestaurantTableStatus.AVAILABLE
                && hasMergedMembers(tableId)) {
            // Đây là nhánh rất quan trọng:
            // nếu bàn gốc của nhóm gộp đã dọn xong và chuyển về AVAILABLE,
            // toàn bộ nhóm phải cùng quay về AVAILABLE và bỏ liên kết mergedIntoTable.
            List<RestaurantTable> mergedGroup = loadMergedGroup(table);
            for (RestaurantTable groupTable : mergedGroup) {
                groupTable.setStatus(RestaurantTableStatus.AVAILABLE);
                if (!groupTable.getId().equals(table.getId())) {
                    groupTable.setMergedIntoTable(null);
                }
            }
            restaurantTableRepository.saveAll(mergedGroup);
            Map<Integer, List<RestaurantTable>> mergedGroupMap = loadMergedGroupMap(List.of(table));
            return toResponse(table, mergedGroupMap);
        }

        table.setStatus(targetStatus);
        restaurantTableRepository.save(table);
        Map<Integer, List<RestaurantTable>> mergedGroupMap = loadMergedGroupMap(List.of(table));
        return toResponse(table, mergedGroupMap);
    }

    @Transactional(readOnly = true)
    public RestaurantTable getTableEntity(Integer tableId) {
        return restaurantTableRepository.findDetailById(tableId)
                .orElseThrow(() -> new NotFoundException("Table not found"));
    }

    private Branch resolveBranch(Integer branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private DiningArea resolveArea(Integer areaId, Integer branchId) {
        if (areaId == null) {
            return null;
        }

        DiningArea area = diningAreaRepository.findByIdAndBranch_Id(areaId, branchId)
                .orElseThrow(() -> new ConflictException("Dining area does not belong to the selected branch"));
        if (!Boolean.TRUE.equals(area.getActive())) {
            throw new ConflictException("Cannot assign a table to an inactive dining area");
        }
        return area;
    }

    private void ensureTableNumberAvailable(Integer branchId, String tableNumber, Integer currentTableId) {
        boolean exists = currentTableId == null
                ? restaurantTableRepository.existsByBranch_IdAndTableNumberIgnoreCase(branchId, tableNumber)
                : restaurantTableRepository.existsByBranch_IdAndTableNumberIgnoreCaseAndIdNot(branchId, tableNumber, currentTableId);

        if (exists) {
            throw new ConflictException("Table number already exists in the selected branch");
        }
    }

    private void validateStatusTransition(RestaurantTableStatus currentStatus, RestaurantTableStatus targetStatus) {
        if (currentStatus == targetStatus) {
            throw new ConflictException("Table is already in the target status");
        }
        // OCCUPIED là trạng thái do luồng order tạo ra.
        // Nếu cho phép set tay ở đây sẽ rất dễ làm lệch dữ liệu giữa table và order.
        if (targetStatus == RestaurantTableStatus.OCCUPIED) {
            throw new ConflictException("Occupied status is managed by order workflow");
        }
        if (currentStatus == RestaurantTableStatus.AVAILABLE && targetStatus == RestaurantTableStatus.RESERVED) {
            return;
        }
        if (currentStatus == RestaurantTableStatus.RESERVED && targetStatus == RestaurantTableStatus.AVAILABLE) {
            return;
        }
        if (currentStatus == RestaurantTableStatus.CLEANING && targetStatus == RestaurantTableStatus.AVAILABLE) {
            return;
        }

        throw new ConflictException("Unsupported table status transition");
    }

    private Integer resolveAccessibleBranchId(Integer branchId, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_MANAGER)) {
            return branchId;
        }

        User currentUserEntity = userRepository.findEmployeeDetailById(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user not found"));

        if (currentUserEntity.getProfile() == null || currentUserEntity.getProfile().getBranch() == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }

        Integer ownBranchId = currentUserEntity.getProfile().getBranch().getId();
        if (branchId != null && !branchId.equals(ownBranchId)) {
            throw new ForbiddenException("You do not have permission to view another branch");
        }

        return ownBranchId;
    }

    private RestaurantTableResponse toResponse(RestaurantTable table) {
        return toResponse(table, loadMergedGroupMap(List.of(table)));
    }

    private RestaurantTableResponse toResponse(RestaurantTable table,
                                               Map<Integer, List<RestaurantTable>> mergedGroupMap) {
        Integer mergeRootId = extractMergeRootId(table);
        // Một response bàn luôn phải biết:
        // - nó có thuộc nhóm gộp không
        // - ai là bàn gốc
        // - tên hiển thị của cả nhóm là gì
        List<RestaurantTable> mergedGroup = mergedGroupMap.getOrDefault(mergeRootId, List.of(table));
        String displayName = buildTableDisplayName(mergedGroup, mergeRootId);
        List<Integer> mergedTableIds = mergedGroup.stream()
                .map(RestaurantTable::getId)
                .toList();
        List<String> mergedTableNames = mergedGroup.stream()
                .map(RestaurantTable::getTableNumber)
                .toList();
        boolean merged = mergedGroup.size() > 1;

        return new RestaurantTableResponse(
                table.getId(),
                table.getBranch().getId(),
                table.getBranch().getName(),
                table.getArea() != null ? table.getArea().getId() : null,
                table.getArea() != null ? table.getArea().getName() : null,
                table.getTableNumber(),
                table.getCapacity(),
                table.getPosX(),
                table.getPosY(),
                table.getWidth(),
                table.getHeight(),
                table.getDisplayOrder(),
                table.getStatus().name(),
                merged,
                merged && table.getId().equals(mergeRootId),
                merged ? mergeRootId : null,
                merged ? findRootTableName(mergedGroup, mergeRootId) : null,
                displayName,
                mergedTableIds,
                mergedTableNames
        );
    }

    private Map<Integer, List<RestaurantTable>> loadMergedGroupMap(List<RestaurantTable> tables) {
        if (tables.isEmpty()) {
            return Map.of();
        }

        List<Integer> rootIds = tables.stream()
                .map(this::extractMergeRootId)
                .distinct()
                .toList();

        // Chỉ cần biết root id của từng nhóm, sau đó lấy toàn bộ thành viên của các nhóm đó một lần.
        List<RestaurantTable> groupedTables = restaurantTableRepository.findAllInMergedGroups(rootIds);
        Map<Integer, List<RestaurantTable>> mergedGroupMap = new LinkedHashMap<>();
        for (RestaurantTable groupedTable : groupedTables) {
            Integer rootId = extractMergeRootId(groupedTable);
            mergedGroupMap.computeIfAbsent(rootId, ignored -> new ArrayList<>()).add(groupedTable);
        }

        mergedGroupMap.values().forEach(group -> group.sort(TABLE_GROUP_COMPARATOR));
        return mergedGroupMap;
    }

    private Integer extractMergeRootId(RestaurantTable table) {
        return table.getMergedIntoTableId() != null ? table.getMergedIntoTableId() : table.getId();
    }

    private boolean isMergedMember(RestaurantTable table) {
        return table.getMergedIntoTableId() != null;
    }

    private boolean hasMergedMembers(Integer tableId) {
        return restaurantTableRepository.existsByMergedIntoTable_Id(tableId);
    }

    private List<RestaurantTable> loadMergedGroup(RestaurantTable rootTable) {
        // Hàm này giả định đầu vào là bàn gốc hoặc sẽ được caller quy về bàn gốc trước đó.
        List<RestaurantTable> groupedTables = restaurantTableRepository.findAllInMergedGroups(List.of(rootTable.getId()));
        if (groupedTables.isEmpty()) {
            return List.of(rootTable);
        }
        groupedTables.sort(TABLE_GROUP_COMPARATOR);
        return groupedTables;
    }

    private String buildTableDisplayName(List<RestaurantTable> mergedGroup, Integer mergeRootId) {
        // Ví dụ nhóm gồm bàn gốc T07 và bàn thành viên T06 sẽ hiển thị thành T07&T06.
        // Bàn gốc luôn đứng trước để tên hiển thị ổn định.
        List<RestaurantTable> orderedGroup = mergedGroup.stream()
                .sorted((left, right) -> {
                    if (left.getId().equals(mergeRootId)) {
                        return -1;
                    }
                    if (right.getId().equals(mergeRootId)) {
                        return 1;
                    }
                    return TABLE_GROUP_COMPARATOR.compare(left, right);
                })
                .toList();

        return orderedGroup.stream()
                .map(RestaurantTable::getTableNumber)
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String findRootTableName(List<RestaurantTable> mergedGroup, Integer mergeRootId) {
        return mergedGroup.stream()
                .filter(table -> table.getId().equals(mergeRootId))
                .map(RestaurantTable::getTableNumber)
                .findFirst()
                .orElse(null);
    }

    private RestaurantTableStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parseRequiredStatus(status);
    }

    private RestaurantTableStatus parseRequiredStatus(String status) {
        try {
            return RestaurantTableStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Unsupported table status");
        }
    }

    private String normalizeTableNumber(String tableNumber) {
        if (!StringUtils.hasText(tableNumber)) {
            throw new ConflictException("Table number is required");
        }
        return tableNumber.trim();
    }

    private boolean matchesListingKeyword(RestaurantTable table, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return true;
        }

        return containsNormalizedText(table.getTableNumber(), normalizedKeyword)
                || containsNormalizedText(table.getArea() != null ? table.getArea().getName() : null, normalizedKeyword);
    }

    private boolean containsNormalizedText(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private boolean hasRole(CustomUserDetails currentUser, String role) {
        return role.equalsIgnoreCase(currentUser.getRoleName());
    }

    private void requireAnyRole(CustomUserDetails currentUser, String... roles) {
        for (String role : roles) {
            if (hasRole(currentUser, role)) {
                return;
            }
        }
        throw new ForbiddenException("You do not have permission to perform this action");
    }
}
