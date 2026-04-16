/*
  Deterministic reference seed for GoldenHeart Restaurant local API testing.

  This script is designed to run after:
  1. sql/01_reset_local_database.sql
  2. starting Spring Boot once so Hibernate creates schema

  Philosophy:
  - keep bootstrap minimal and conflict-free
  - seed only reference/master data here
  - let Postman create operational data (orders, stock deductions, bills, payments) end-to-end
*/

USE goldenheart_restaurant;

SET @seed_now = NOW();
SET @ops_password_hash = '$2a$12$xAstGzraDiapskRYMG4.uOmSntUw2jzYz0aJPAtszRJePlAem8zF.';

/*
  Minimal identity reference data.
  Roles + admin are still safe to bootstrap from Spring at app startup,
  but we normalize the role catalog here to keep names/descriptions stable.
*/
INSERT INTO roles (id, name, description, created_at, updated_at, deleted_at)
VALUES
    (1, 'ADMIN', 'System administrator', @seed_now, @seed_now, NULL),
    (2, 'MANAGER', 'Restaurant manager', @seed_now, @seed_now, NULL),
    (3, 'STAFF', 'Floor staff', @seed_now, @seed_now, NULL),
    (4, 'KITCHEN', 'Kitchen staff', @seed_now, @seed_now, NULL),
    (5, 'CUSTOMER', 'Customer self-registration role', @seed_now, @seed_now, NULL)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    deleted_at = NULL,
    updated_at = @seed_now;

INSERT INTO measurement_units (id, code, name, symbol, active_code, active_symbol, description, created_at, updated_at, deleted_at)
VALUES
    (1, 'KG', 'Kilogram', 'kg', 'KG', 'kg', 'Don vi tinh cho thit, rau cu, bot va nguyen lieu khoi luong lon', @seed_now, @seed_now, NULL),
    (2, 'PIECE', 'Piece', 'piece', 'PIECE', 'piece', 'Don vi tinh theo tung cai, qua, chai', @seed_now, @seed_now, NULL),
    (3, 'LITER', 'Liter', 'l', 'LITER', 'l', 'Don vi tinh theo lit', @seed_now, @seed_now, NULL),
    (4, 'GRAM', 'Gram', 'g', 'GRAM', 'g', 'Don vi tinh nho hon kilogram', @seed_now, @seed_now, NULL),
    (5, 'MILLILITER', 'Milliliter', 'ml', 'MILLILITER', 'ml', 'Don vi tinh nho hon liter', @seed_now, @seed_now, NULL)
ON DUPLICATE KEY UPDATE
    code = VALUES(code),
    name = VALUES(name),
    symbol = VALUES(symbol),
    active_code = VALUES(active_code),
    active_symbol = VALUES(active_symbol),
    description = VALUES(description),
    deleted_at = NULL,
    updated_at = @seed_now;

/*
  Restaurant master data.
  There is no CRUD API for restaurant/branch/area/table/category right now,
  so we seed deterministic IDs for Postman + guide alignment.
*/
INSERT INTO restaurants (id, name, address, phone)
VALUES
    (1, 'GoldenHeart Restaurant', '1 Nguyen Hue, District 1, Ho Chi Minh City', '0900000001')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    address = VALUES(address),
    phone = VALUES(phone);

INSERT INTO branches (id, restaurant_id, name, address, phone)
VALUES
    (1, 1, 'GoldenHeart D1', '1 Nguyen Hue, District 1, Ho Chi Minh City', '0900000002'),
    (2, 1, 'GoldenHeart Thu Duc', '88 Vo Van Ngan, Thu Duc, Ho Chi Minh City', '0900000003')
ON DUPLICATE KEY UPDATE
    restaurant_id = VALUES(restaurant_id),
    name = VALUES(name),
    address = VALUES(address),
    phone = VALUES(phone);

INSERT INTO dining_areas (id, branch_id, name, code, display_order, active)
VALUES
    (1, 1, 'Main Hall', 'MAIN', 1, TRUE),
    (2, 1, 'Upstairs', 'UP', 2, TRUE),
    (3, 2, 'Garden', 'GARDEN', 1, TRUE)
ON DUPLICATE KEY UPDATE
    branch_id = VALUES(branch_id),
    name = VALUES(name),
    code = VALUES(code),
    display_order = VALUES(display_order),
    active = VALUES(active);

INSERT INTO tables (id, branch_id, area_id, table_number, capacity, pos_x, pos_y, width, height, display_order, status)
VALUES
    (1, 1, 1, 'T01', 2, 10.00, 10.00, 80.00, 80.00, 1, 'AVAILABLE'),
    (2, 1, 1, 'T02', 4, 110.00, 10.00, 100.00, 100.00, 2, 'AVAILABLE'),
    (3, 1, 1, 'T03', 4, 230.00, 10.00, 100.00, 100.00, 3, 'AVAILABLE'),
    (4, 1, 2, 'T04', 6, 10.00, 130.00, 120.00, 120.00, 4, 'AVAILABLE'),
    (5, 1, 2, 'T05', 6, 150.00, 130.00, 120.00, 120.00, 5, 'AVAILABLE'),
    (6, 2, 3, 'TD01', 4, 10.00, 10.00, 100.00, 100.00, 1, 'AVAILABLE')
ON DUPLICATE KEY UPDATE
    branch_id = VALUES(branch_id),
    area_id = VALUES(area_id),
    table_number = VALUES(table_number),
    capacity = VALUES(capacity),
    pos_x = VALUES(pos_x),
    pos_y = VALUES(pos_y),
    width = VALUES(width),
    height = VALUES(height),
    display_order = VALUES(display_order),
    status = VALUES(status);

INSERT INTO categories (id, name, description)
VALUES
    (1, 'Pho', 'Vietnamese noodle soup'),
    (2, 'Drinks', 'Beverages'),
    (3, 'Desserts', 'Sweet ending menu')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description);

/*
  Baseline employee accounts for stable role-based testing.
  Password for manager/staff/kitchen seed accounts: Password123
*/
INSERT INTO users (id, username, password_hash, role_id, status, created_at, updated_at, deleted_at)
SELECT
    2,
    'manager.d1',
    @ops_password_hash,
    r.id,
    'ACTIVE',
    @seed_now,
    @seed_now,
    NULL
FROM roles r
WHERE UPPER(r.name) = 'MANAGER'
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    role_id = VALUES(role_id),
    status = VALUES(status),
    deleted_at = NULL,
    updated_at = @seed_now;

INSERT INTO users (id, username, password_hash, role_id, status, created_at, updated_at, deleted_at)
SELECT
    3,
    'staff.d1',
    @ops_password_hash,
    r.id,
    'ACTIVE',
    @seed_now,
    @seed_now,
    NULL
FROM roles r
WHERE UPPER(r.name) = 'STAFF'
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    role_id = VALUES(role_id),
    status = VALUES(status),
    deleted_at = NULL,
    updated_at = @seed_now;

INSERT INTO users (id, username, password_hash, role_id, status, created_at, updated_at, deleted_at)
SELECT
    4,
    'kitchen.d1',
    @ops_password_hash,
    r.id,
    'ACTIVE',
    @seed_now,
    @seed_now,
    NULL
FROM roles r
WHERE UPPER(r.name) = 'KITCHEN'
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    role_id = VALUES(role_id),
    status = VALUES(status),
    deleted_at = NULL,
    updated_at = @seed_now;

INSERT INTO user_profiles (
    user_id, full_name, employee_code, phone, email, active_phone, active_email,
    address, branch_id, date_of_birth, gender, hire_date, salary, internal_notes,
    created_at, updated_at, deleted_at
)
VALUES
    (2, 'Manager D1', 'EMP-MGR-D1', '0901000001', 'manager.d1@goldenheart.local', '0901000001', 'manager.d1@goldenheart.local',
     'District 1, Ho Chi Minh City', 1, '1992-02-20', 'Male', '2026-04-01', 18000000.00, 'Seeded manager account for E2E testing',
     @seed_now, @seed_now, NULL),
    (3, 'Staff D1', 'EMP-STF-D1', '0901000002', 'staff.d1@goldenheart.local', '0901000002', 'staff.d1@goldenheart.local',
     'District 1, Ho Chi Minh City', 1, '1998-06-15', 'Female', '2026-04-05', 9000000.00, 'Seeded staff account for E2E testing',
     @seed_now, @seed_now, NULL),
    (4, 'Kitchen D1', 'EMP-KIT-D1', '0901000003', 'kitchen.d1@goldenheart.local', '0901000003', 'kitchen.d1@goldenheart.local',
     'District 1, Ho Chi Minh City', 1, '1995-09-18', 'Male', '2026-04-05', 11000000.00, 'Seeded kitchen account for E2E testing',
     @seed_now, @seed_now, NULL)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    employee_code = VALUES(employee_code),
    phone = VALUES(phone),
    email = VALUES(email),
    active_phone = VALUES(active_phone),
    active_email = VALUES(active_email),
    address = VALUES(address),
    branch_id = VALUES(branch_id),
    date_of_birth = VALUES(date_of_birth),
    gender = VALUES(gender),
    hire_date = VALUES(hire_date),
    salary = VALUES(salary),
    internal_notes = VALUES(internal_notes),
    deleted_at = NULL,
    updated_at = @seed_now;

/*
  Customer master data.
  seed customer id = 1 is used in the order -> kitchen -> billing Postman flow.
*/
INSERT INTO customers (
    id, customer_code, name, phone, email, active_phone, active_email,
    loyalty_points, address, date_of_birth, gender, note, last_visit_at,
    created_at, updated_at, deleted_at
)
VALUES
    (1, 'CUS-REF-001', 'Pham Thi Lan', '0912000001', 'pham.thi.lan@goldenheart.local', '0912000001', 'pham.thi.lan@goldenheart.local',
     120, 'District 1, Ho Chi Minh City', '1994-03-12', 'Female', 'Seeded operational customer for order/billing flow', NULL,
     @seed_now, @seed_now, NULL),
    (2, 'CUS-REF-002', 'Nguyen Van Minh', '0912000002', 'nguyen.van.minh@goldenheart.local', '0912000002', 'nguyen.van.minh@goldenheart.local',
     45, 'Thu Duc, Ho Chi Minh City', '1990-11-03', 'Male', 'Additional seeded customer for list/read checks', NULL,
     @seed_now, @seed_now, NULL)
ON DUPLICATE KEY UPDATE
    customer_code = VALUES(customer_code),
    name = VALUES(name),
    phone = VALUES(phone),
    email = VALUES(email),
    active_phone = VALUES(active_phone),
    active_email = VALUES(active_email),
    loyalty_points = VALUES(loyalty_points),
    address = VALUES(address),
    date_of_birth = VALUES(date_of_birth),
    gender = VALUES(gender),
    note = VALUES(note),
    last_visit_at = VALUES(last_visit_at),
    deleted_at = NULL,
    updated_at = @seed_now;

/*
  Ingredient + inventory seed.
  ingredient id = 7 is intentionally low/out-of-stock to make /inventory/alerts deterministic.
*/
INSERT INTO ingredients (id, name, unit_id, unit, description)
VALUES
    (1, 'Beef', 1, 'kg', 'Beef for pho bo'),
    (2, 'Rice Noodle', 1, 'kg', 'Rice noodle for pho'),
    (3, 'Broth', 3, 'l', 'Pho broth base'),
    (4, 'Onion', 4, 'g', 'Sliced onion'),
    (5, 'Chicken', 1, 'kg', 'Chicken for pho ga'),
    (6, 'Herb Mix', 4, 'g', 'Fresh herbs'),
    (7, 'Bean Sprout', 1, 'kg', 'Alert-only low stock ingredient')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    unit_id = VALUES(unit_id),
    unit = VALUES(unit),
    description = VALUES(description);

INSERT INTO inventory (
    id, branch_id, ingredient_id, active_record_key, quantity, min_stock_level,
    reorder_level, average_unit_cost, last_receipt_at, last_counted_at,
    created_at, updated_at, deleted_at
)
VALUES
    (1, 1, 1, 'ACTIVE', 15.00, 2.00, 4.00, 220000.00, @seed_now, NULL, @seed_now, @seed_now, NULL),
    (2, 1, 2, 'ACTIVE', 25.00, 3.00, 5.00, 45000.00, @seed_now, NULL, @seed_now, @seed_now, NULL),
    (3, 1, 3, 'ACTIVE', 60.00, 10.00, 20.00, 15000.00, @seed_now, NULL, @seed_now, @seed_now, NULL),
    (4, 1, 4, 'ACTIVE', 5000.00, 500.00, 1000.00, 30.00, @seed_now, NULL, @seed_now, @seed_now, NULL),
    (5, 1, 5, 'ACTIVE', 12.00, 2.00, 4.00, 160000.00, @seed_now, NULL, @seed_now, @seed_now, NULL),
    (6, 1, 6, 'ACTIVE', 3000.00, 200.00, 400.00, 45.00, @seed_now, NULL, @seed_now, @seed_now, NULL),
    (7, 1, 7, 'ACTIVE', 0.00, 1.00, 2.00, 28000.00, @seed_now, NULL, @seed_now, @seed_now, NULL)
ON DUPLICATE KEY UPDATE
    branch_id = VALUES(branch_id),
    ingredient_id = VALUES(ingredient_id),
    active_record_key = 'ACTIVE',
    quantity = VALUES(quantity),
    min_stock_level = VALUES(min_stock_level),
    reorder_level = VALUES(reorder_level),
    average_unit_cost = VALUES(average_unit_cost),
    last_receipt_at = VALUES(last_receipt_at),
    last_counted_at = VALUES(last_counted_at),
    deleted_at = NULL,
    updated_at = @seed_now;

/*
  Menu + recipe seed for operational flow.
*/
INSERT INTO menu_items (id, branch_id, category_id, name, description, price, status)
VALUES
    (1, 1, 1, 'Pho Bo Tai', 'Seeded pho bo tai for order, kitchen and billing flow', 85000.00, 'AVAILABLE'),
    (2, 1, 1, 'Pho Ga', 'Seeded pho ga for order, kitchen and billing flow', 79000.00, 'AVAILABLE'),
    (3, 1, 2, 'Tra Dao', 'Seeded drink item for menu listing checks', 35000.00, 'AVAILABLE')
ON DUPLICATE KEY UPDATE
    branch_id = VALUES(branch_id),
    category_id = VALUES(category_id),
    name = VALUES(name),
    description = VALUES(description),
    price = VALUES(price),
    status = VALUES(status);

INSERT INTO recipes (menu_item_id, ingredient_id, quantity)
VALUES
    (1, 1, 0.20),
    (1, 2, 0.15),
    (1, 3, 0.50),
    (1, 4, 20.00),
    (2, 5, 0.18),
    (2, 2, 0.14),
    (2, 3, 0.45),
    (2, 6, 15.00),
    (3, 3, 0.10)
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity);

SELECT
    'Reference seed completed successfully.' AS status,
    'Seeded manager/staff/kitchen password = Password123' AS seeded_password_hint,
    'Seeded customer id for operational order flow = 1' AS customer_hint,
    'Seeded table ids for branch 1 = T01..T05 => 1..5' AS table_hint,
    'Seeded menu ids = Pho Bo Tai(1), Pho Ga(2), Tra Dao(3)' AS menu_hint;
