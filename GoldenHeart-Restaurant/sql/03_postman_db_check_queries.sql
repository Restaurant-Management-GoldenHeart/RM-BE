/*
  Database verification queries for the refreshed Postman collection.
  Run these after the collection or after a specific folder to inspect actual DB state.
*/

USE goldenheart_restaurant;

SELECT 'roles' AS section;
SELECT id, name, description, deleted_at
FROM roles
ORDER BY id;

SELECT 'users_and_profiles' AS section;
SELECT
    u.id,
    u.username,
    u.status,
    r.name AS role_name,
    up.full_name,
    up.employee_code,
    up.email,
    up.phone,
    up.branch_id,
    up.deleted_at AS profile_deleted_at,
    u.deleted_at AS user_deleted_at
FROM users u
JOIN roles r ON r.id = u.role_id
LEFT JOIN user_profiles up ON up.user_id = u.id
ORDER BY u.id;

SELECT 'refresh_tokens' AS section;
SELECT
    id,
    user_id,
    revoked,
    expires_at,
    created_at,
    last_used_at,
    revoked_at
FROM refresh_tokens
ORDER BY id DESC;

SELECT 'restaurants_branches_areas_tables' AS section;
SELECT
    rt.id AS restaurant_id,
    rt.name AS restaurant_name,
    b.id AS branch_id,
    b.name AS branch_name,
    da.id AS area_id,
    da.name AS area_name,
    t.id AS table_id,
    t.table_number,
    t.status,
    t.display_order
FROM restaurants rt
LEFT JOIN branches b ON b.restaurant_id = rt.id
LEFT JOIN dining_areas da ON da.branch_id = b.id
LEFT JOIN tables t ON t.area_id = da.id
ORDER BY rt.id, b.id, da.id, t.id;

SELECT 'customers' AS section;
SELECT
    id,
    customer_code,
    name,
    phone,
    email,
    loyalty_points,
    last_visit_at,
    deleted_at
FROM customers
ORDER BY id;

SELECT 'measurement_units' AS section;
SELECT id, code, name, symbol, active_code, active_symbol, deleted_at
FROM measurement_units
ORDER BY id;

SELECT 'ingredients' AS section;
SELECT
    i.id,
    i.name,
    i.unit_id,
    mu.code AS unit_code,
    mu.symbol AS unit_symbol,
    i.unit AS legacy_unit,
    i.description
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
    inv.active_record_key,
    inv.deleted_at
FROM inventory inv
JOIN branches b ON b.id = inv.branch_id
JOIN ingredients i ON i.id = inv.ingredient_id
LEFT JOIN measurement_units mu ON mu.id = i.unit_id
ORDER BY inv.id;

SELECT 'inventory_alerts_expected_low_stock' AS section;
SELECT
    inv.id AS inventory_id,
    b.name AS branch_name,
    i.name AS ingredient_name,
    inv.quantity,
    inv.min_stock_level,
    inv.reorder_level
FROM inventory inv
JOIN branches b ON b.id = inv.branch_id
JOIN ingredients i ON i.id = inv.ingredient_id
WHERE inv.deleted_at IS NULL
  AND COALESCE(inv.quantity, 0) <= COALESCE(inv.min_stock_level, 0)
ORDER BY inv.quantity ASC, inv.id ASC;

SELECT 'inventory_action_logs' AS section;
SELECT
    id,
    inventory_id,
    action_type,
    acted_by_username,
    branch_name,
    ingredient_name,
    before_quantity,
    after_quantity,
    summary,
    occurred_at
FROM inventory_action_logs
ORDER BY id DESC;

SELECT 'menu_items_and_recipes' AS section;
SELECT
    mi.id AS menu_item_id,
    mi.name AS menu_item_name,
    mi.branch_id,
    mi.category_id,
    mi.price,
    mi.status,
    r.id AS recipe_id,
    r.ingredient_id,
    i.name AS ingredient_name,
    r.quantity AS recipe_quantity
FROM menu_items mi
LEFT JOIN recipes r ON r.menu_item_id = mi.id
LEFT JOIN ingredients i ON i.id = r.ingredient_id
ORDER BY mi.id, r.id;

SELECT 'orders_and_items' AS section;
SELECT
    o.id AS order_id,
    o.branch_id,
    o.table_id,
    t.table_number,
    o.customer_id,
    c.name AS customer_name,
    o.created_by,
    o.status AS order_status,
    o.created_at,
    o.closed_at,
    oi.id AS order_item_id,
    oi.menu_item_id,
    mi.name AS menu_item_name,
    oi.quantity,
    oi.price,
    oi.status AS order_item_status,
    oi.note
FROM orders o
LEFT JOIN tables t ON t.id = o.table_id
LEFT JOIN customers c ON c.id = o.customer_id
LEFT JOIN order_items oi ON oi.order_id = o.id
LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id
ORDER BY o.id, oi.id;

SELECT 'bills_and_payments' AS section;
SELECT
    b.id AS bill_id,
    b.order_id,
    b.status,
    b.subtotal,
    b.tax,
    b.discount,
    b.total,
    b.cost_of_goods_sold,
    b.gross_profit,
    p.id AS payment_id,
    p.amount,
    p.method,
    p.paid_at
FROM bills b
LEFT JOIN payments p ON p.bill_id = b.id
ORDER BY b.id, p.id;

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
    sm.unit_cost,
    sm.total_cost,
    sm.occurred_at,
    sm.note
FROM stock_movements sm
JOIN ingredients i ON i.id = sm.ingredient_id
ORDER BY sm.id DESC;

SELECT 'seed_reference_summary' AS section;
SELECT
    (SELECT COUNT(*) FROM branches) AS branch_count,
    (SELECT COUNT(*) FROM tables) AS table_count,
    (SELECT COUNT(*) FROM categories) AS category_count,
    (SELECT COUNT(*) FROM ingredients) AS ingredient_count,
    (SELECT COUNT(*) FROM inventory WHERE deleted_at IS NULL) AS active_inventory_count,
    (SELECT COUNT(*) FROM menu_items) AS menu_item_count,
    (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL) AS active_user_count;
