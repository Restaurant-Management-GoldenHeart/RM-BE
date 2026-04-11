USE goldenheart_restaurant;

INSERT INTO measurement_units (
    id, code, name, symbol, active_code, active_symbol, description, created_at, updated_at, deleted_at
)
SELECT 1, 'KG', 'Kilogram', 'kg', 'KG', 'kg', 'Don vi tinh cho thit, rau cu, bot...', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM measurement_units WHERE id = 1 OR code = 'KG');

INSERT INTO measurement_units (
    id, code, name, symbol, active_code, active_symbol, description, created_at, updated_at, deleted_at
)
SELECT 2, 'PIECE', 'Piece', 'piece', 'PIECE', 'piece', 'Don vi tinh theo tung cai, qua, chai...', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM measurement_units WHERE id = 2 OR code = 'PIECE');

INSERT INTO measurement_units (
    id, code, name, symbol, active_code, active_symbol, description, created_at, updated_at, deleted_at
)
SELECT 3, 'LITER', 'Liter', 'l', 'LITER', 'l', 'Don vi tinh theo lit', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM measurement_units WHERE id = 3 OR code = 'LITER');

INSERT INTO measurement_units (
    id, code, name, symbol, active_code, active_symbol, description, created_at, updated_at, deleted_at
)
SELECT 4, 'GRAM', 'Gram', 'g', 'GRAM', 'g', 'Don vi tinh theo gram', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM measurement_units WHERE id = 4 OR code = 'GRAM');

INSERT INTO measurement_units (
    id, code, name, symbol, active_code, active_symbol, description, created_at, updated_at, deleted_at
)
SELECT 5, 'MILLILITER', 'Milliliter', 'ml', 'MILLILITER', 'ml', 'Don vi tinh theo ml', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM measurement_units WHERE id = 5 OR code = 'MILLILITER');


INSERT INTO restaurants (id, name, address, phone)
SELECT 1, 'GoldenHeart Restaurant', '1 Nguyen Hue, District 1, Ho Chi Minh City', '0900000001'
WHERE NOT EXISTS (SELECT 1 FROM restaurants WHERE id = 1);

INSERT INTO branches (id, restaurant_id, name, address, phone)
SELECT 1, 1, 'GoldenHeart D1', '1 Nguyen Hue, District 1, Ho Chi Minh City', '0900000002'
WHERE EXISTS (SELECT 1 FROM restaurants WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM branches WHERE id = 1);

INSERT INTO branches (id, restaurant_id, name, address, phone)
SELECT 2, 1, 'GoldenHeart Thu Duc', '88 Vo Van Ngan, Thu Duc, Ho Chi Minh City', '0900000003'
WHERE EXISTS (SELECT 1 FROM restaurants WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM branches WHERE id = 2);


INSERT INTO categories (id, name, description)
SELECT 1, 'Pho', 'Mon pho Viet Nam'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 1);

INSERT INTO categories (id, name, description)
SELECT 2, 'Mon Them', 'Cac mon phu va nguyen lieu bo sung'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 2);


INSERT INTO customers (
    customer_code, name, phone, email, active_phone, active_email,
    loyalty_points, address, date_of_birth, gender, note, created_at, updated_at, deleted_at
)
SELECT
    'CUS901', 'Pham Van Test 1', '0933333301', 'seed.customer1@example.com',
    '0933333301', 'seed.customer1@example.com',
    10, 'HCM City', '1999-01-01', 'Male', 'Khach hang seed local', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE customer_code = 'CUS901');

INSERT INTO customers (
    customer_code, name, phone, email, active_phone, active_email,
    loyalty_points, address, date_of_birth, gender, note, created_at, updated_at, deleted_at
)
SELECT
    'CUS902', 'Le Thi Test 2', '0933333302', 'seed.customer2@example.com',
    '0933333302', 'seed.customer2@example.com',
    20, 'Da Nang', '1998-02-02', 'Female', 'Khach hang seed local', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE customer_code = 'CUS902');

INSERT INTO customers (
    customer_code, name, phone, email, active_phone, active_email,
    loyalty_points, address, date_of_birth, gender, note, created_at, updated_at, deleted_at
)
SELECT
    'CUS903', 'Nguyen Test 3', '0933333303', 'seed.customer3@example.com',
    '0933333303', 'seed.customer3@example.com',
    30, 'Ha Noi', '1997-03-03', 'Male', 'Khach hang seed local', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE customer_code = 'CUS903');


INSERT INTO ingredients (id, name, unit_id, unit, description)
SELECT 1, 'Beef', 1, 'kg', 'Thit bo de nau pho'
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE id = 1);

INSERT INTO ingredients (id, name, unit_id, unit, description)
SELECT 2, 'Rice Noodle', 1, 'kg', 'Banh pho'
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE id = 2);

INSERT INTO ingredients (id, name, unit_id, unit, description)
SELECT 3, 'Broth', 3, 'l', 'Nuoc dung pho'
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE id = 3);

INSERT INTO ingredients (id, name, unit_id, unit, description)
SELECT 4, 'Onion', 4, 'g', 'Hanh'
WHERE NOT EXISTS (SELECT 1 FROM ingredients WHERE id = 4);

INSERT INTO inventory (
    id, branch_id, ingredient_id, active_record_key, quantity, min_stock_level,
    reorder_level, average_unit_cost, last_receipt_at, last_counted_at, created_at, updated_at, deleted_at
)
SELECT 1, 1, 1, 'ACTIVE', 10.00, 2.00, 3.00, 220000.00, NOW(), NOW(), NOW(), NOW(), NULL
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE id = 1);

INSERT INTO inventory (
    id, branch_id, ingredient_id, active_record_key, quantity, min_stock_level,
    reorder_level, average_unit_cost, last_receipt_at, last_counted_at, created_at, updated_at, deleted_at
)
SELECT 2, 1, 2, 'ACTIVE', 20.00, 3.00, 5.00, 45000.00, NOW(), NOW(), NOW(), NOW(), NULL
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE id = 2);

INSERT INTO inventory (
    id, branch_id, ingredient_id, active_record_key, quantity, min_stock_level,
    reorder_level, average_unit_cost, last_receipt_at, last_counted_at, created_at, updated_at, deleted_at
)
SELECT 3, 1, 3, 'ACTIVE', 40.00, 5.00, 8.00, 15000.00, NOW(), NOW(), NOW(), NOW(), NULL
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE id = 3);

INSERT INTO inventory (
    id, branch_id, ingredient_id, active_record_key, quantity, min_stock_level,
    reorder_level, average_unit_cost, last_receipt_at, last_counted_at, created_at, updated_at, deleted_at
)
SELECT 4, 1, 4, 'ACTIVE', 5.00, 1.00, 2.00, 30000.00, NOW(), NOW(), NOW(), NOW(), NULL
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE id = 4);

INSERT INTO menu_items (id, branch_id, category_id, name, description, price, status)
SELECT 1, 1, 1, 'Pho Bo Tai', 'Pho bo tai de test menu va kitchen', 85000.00, 'AVAILABLE'
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND EXISTS (SELECT 1 FROM categories WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE id = 1);

INSERT INTO recipes (menu_item_id, ingredient_id, quantity)
SELECT 1, 1, 0.20
WHERE EXISTS (SELECT 1 FROM menu_items WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM recipes WHERE menu_item_id = 1 AND ingredient_id = 1);

INSERT INTO recipes (menu_item_id, ingredient_id, quantity)
SELECT 1, 2, 0.15
WHERE EXISTS (SELECT 1 FROM menu_items WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM recipes WHERE menu_item_id = 1 AND ingredient_id = 2);

INSERT INTO orders (
    id, branch_id, table_id, customer_id, created_by, status, created_at, closed_at
)
SELECT
    1,
    1,
    NULL,
    NULL,
    u.id,
    'PENDING',
    NOW(),
    NULL
FROM users u
WHERE lower(u.username) = 'admin'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE id = 1);

INSERT INTO order_items (
    id, order_id, menu_item_id, quantity, price, status, note
)
SELECT
    1,
    1,
    1,
    2,
    85000.00,
    'PENDING',
    'Seeded for kitchen complete API test'
WHERE EXISTS (SELECT 1 FROM orders WHERE id = 1)
  AND EXISTS (SELECT 1 FROM menu_items WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM order_items WHERE id = 1);


SELECT 'Seed xong. CÃ³ thá»ƒ test vá»›i branchId=1, categoryId=1, menuItemId=1, orderItemId=1, inventoryId=1.' AS huong_dan;

