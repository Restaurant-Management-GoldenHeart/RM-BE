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
    u.role_id,
    u.created_at,
    u.updated_at,
    u.deleted_at,
    up.full_name,
    up.employee_code,
    up.email,
    up.phone,
    up.branch_id,
    up.deleted_at AS profile_deleted_at
FROM users u
LEFT JOIN user_profiles up ON up.user_id = u.id
ORDER BY u.id;

SELECT 'refresh_tokens' AS section;
SELECT
    id,
    user_id,
    token_hash,
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

SELECT 'restaurants' AS section;
SELECT id, name, address, phone
FROM restaurants
ORDER BY id;

SELECT 'branches' AS section;
SELECT id, restaurant_id, name, address, phone
FROM branches
ORDER BY id;

SELECT 'categories' AS section;
SELECT id, name, description
FROM categories
ORDER BY id;

SELECT 'ingredients' AS section;
SELECT id, name, unit
FROM ingredients
ORDER BY id;

SELECT 'inventory' AS section;
SELECT id, branch_id, ingredient_id, quantity, average_unit_cost
FROM inventory
ORDER BY id;

SELECT 'menu_items' AS section;
SELECT id, branch_id, category_id, name, price, status
FROM menu_items
ORDER BY id;

SELECT 'recipes' AS section;
SELECT id, menu_item_id, ingredient_id, quantity
FROM recipes
ORDER BY id;

SELECT 'orders_and_order_items' AS section;
SELECT
    o.id AS order_id,
    o.branch_id,
    o.created_by,
    o.status AS order_status,
    oi.id AS order_item_id,
    oi.menu_item_id,
    oi.quantity,
    oi.status AS order_item_status,
    oi.note
FROM orders o
LEFT JOIN order_items oi ON oi.order_id = o.id
ORDER BY o.id, oi.id;

SELECT 'stock_movements' AS section;
SELECT
    id,
    branch_id,
    ingredient_id,
    order_id,
    order_item_id,
    movement_type,
    quantity_change,
    balance_after,
    total_cost,
    occurred_at
FROM stock_movements
ORDER BY id DESC;
