-- ============================================================
-- FILE 03: QUERY KIá»‚M TRA SAU KHI TEST POSTMAN
-- Má»¥c Ä‘Ã­ch:
--   - Xem nhanh dá»¯ liá»‡u trong DB sau khi gá»i API
--   - Táº­p trung vÃ o auth, customer, menu, inventory, kitchen
-- ============================================================

USE goldenheart_restaurant;

SELECT 'roles' AS section;
SELECT id, name, description, created_at, updated_at, deleted_at
FROM roles
ORDER BY id;

SELECT 'users_and_profiles' AS section;
SELECT
    u.id,
    u.username,
    u.status,
    r.name AS role_name,
    u.created_at,
    u.updated_at,
    up.full_name,
    up.employee_code,
    up.email,
    up.phone,
    up.branch_id
FROM users u
LEFT JOIN roles r ON r.id = u.role_id
LEFT JOIN user_profiles up ON up.user_id = u.id
ORDER BY u.id;

SELECT 'refresh_tokens' AS section;
SELECT
    id,
    user_id,
    expires_at,
    revoked,
    created_at,
    last_used_at,
    revoked_at
FROM refresh_tokens
ORDER BY id DESC;

SELECT 'customers' AS section;
SELECT
    id,
    customer_code,
    name,
    email,
    phone,
    loyalty_points,
    created_at,
    updated_at,
    deleted_at
FROM customers
ORDER BY id;

SELECT 'measurement_units' AS section;
SELECT id, code, name, symbol, active_code, active_symbol
FROM measurement_units
ORDER BY id;

SELECT 'ingredients_with_units' AS section;
SELECT
    i.id,
    i.name,
    i.unit_id,
    mu.code AS unit_code,
    mu.symbol AS unit_symbol,
    i.unit AS legacy_unit
FROM ingredients i
LEFT JOIN measurement_units mu ON mu.id = i.unit_id
ORDER BY i.id;

SELECT 'inventory' AS section;
SELECT
    inv.id,
    inv.branch_id,
    b.name AS branch_name,
    inv.ingredient_id,
    i.name AS ingredient_name,
    mu.symbol AS unit_symbol,
    inv.quantity,
    inv.min_stock_level,
    inv.reorder_level,
    inv.average_unit_cost,
    inv.deleted_at
FROM inventory inv
JOIN branches b ON b.id = inv.branch_id
JOIN ingredients i ON i.id = inv.ingredient_id
LEFT JOIN measurement_units mu ON mu.id = i.unit_id
ORDER BY inv.id;

SELECT 'inventory_low_stock_alerts' AS section;
SELECT
    inv.id AS inventory_id,
    b.name AS branch_name,
    i.name AS ingredient_name,
    mu.symbol AS unit_symbol,
    inv.quantity,
    inv.min_stock_level,
    inv.reorder_level
FROM inventory inv
JOIN branches b ON b.id = inv.branch_id
JOIN ingredients i ON i.id = inv.ingredient_id
LEFT JOIN measurement_units mu ON mu.id = i.unit_id
WHERE inv.deleted_at IS NULL
  AND inv.min_stock_level IS NOT NULL
  AND COALESCE(inv.quantity, 0) <= inv.min_stock_level
ORDER BY inv.quantity ASC, i.name ASC;

SELECT 'inventory_action_logs' AS section;
SELECT
    id,
    inventory_id,
    branch_name,
    ingredient_name,
    action_type,
    acted_by_username,
    before_quantity,
    after_quantity,
    summary,
    occurred_at
FROM inventory_action_logs
ORDER BY id DESC;

SELECT 'menu_items' AS section;
SELECT id, branch_id, category_id, name, price, status
FROM menu_items
ORDER BY id;

SELECT 'recipes' AS section;
SELECT
    r.id,
    r.menu_item_id,
    mi.name AS menu_item_name,
    r.ingredient_id,
    i.name AS ingredient_name,
    r.quantity
FROM recipes r
JOIN menu_items mi ON mi.id = r.menu_item_id
JOIN ingredients i ON i.id = r.ingredient_id
ORDER BY r.id;

SELECT 'orders_and_order_items' AS section;
SELECT
    o.id AS order_id,
    o.branch_id,
    o.created_by,
    o.status AS order_status,
    oi.id AS order_item_id,
    oi.menu_item_id,
    mi.name AS menu_item_name,
    oi.quantity,
    oi.status AS order_item_status,
    oi.note
FROM orders o
LEFT JOIN order_items oi ON oi.order_id = o.id
LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id
ORDER BY o.id, oi.id;

SELECT 'stock_movements' AS section;
SELECT
    sm.id,
    sm.branch_id,
    sm.ingredient_id,
    i.name AS ingredient_name,
    sm.order_id,
    sm.order_item_id,
    sm.movement_type,
    sm.quantity_change,
    sm.balance_after,
    sm.total_cost,
    sm.occurred_at,
    sm.note
FROM stock_movements sm
JOIN ingredients i ON i.id = sm.ingredient_id
ORDER BY sm.id DESC;

