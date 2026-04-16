-- =============================================================================
-- GoldenHeart Restaurant – Full Test Seed Data
-- File: 05_seed_full_test_data.sql
-- Mục đích: Tạo bộ dữ liệu seed chuẩn logic, đủ để test toàn bộ API luồng
--            mà không vi phạm bất kỳ ràng buộc FK / UK nào.
--
-- Thứ tự insert bắt buộc (theo phụ thuộc FK):
--   1. measurement_units
--   2. restaurants
--   3. branches
--   4. dining_areas
--   5. tables
--   6. roles   (bootstrap seeder đã tạo, dùng INSERT IGNORE để safe)
--   7. users + user_profiles
--   8. ingredients
--   9. inventory
--  10. categories
--  11. menu_items + recipes
--  12. customers
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. MEASUREMENT UNITS
--    unique: active_code, active_symbol
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO measurement_units (code, name, symbol, active_code, active_symbol, description, created_at, updated_at)
VALUES
  ('KG',    'Kilogram',    'kg',   'KG',    'kg',   'Đơn vị khối lượng',    NOW(), NOW()),
  ('G',     'Gram',        'g',    'G',     'g',    'Đơn vị khối lượng nhỏ', NOW(), NOW()),
  ('L',     'Lít',         'L',    'L',     'L',    'Đơn vị thể tích',       NOW(), NOW()),
  ('ML',    'Mililít',     'mL',   'ML',    'mL',   'Đơn vị thể tích nhỏ',  NOW(), NOW()),
  ('PCS',   'Cái/Chiếc',  'pcs',  'PCS',   'pcs',  'Đơn vị đếm',            NOW(), NOW()),
  ('BOX',   'Hộp',         'box',  'BOX',   'box',  'Đơn vị đóng gói',       NOW(), NOW()),
  ('BUNCH', 'Bó',          'bunch','BUNCH', 'bunch','Đóng gói rau củ',        NOW(), NOW());

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. RESTAURANT
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO restaurants (id, name, created_at, updated_at)
VALUES (1, 'GoldenHeart Restaurant', NOW(), NOW());

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. BRANCHES
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO branches (id, restaurant_id, name, address, phone)
VALUES
  (1, 1, 'Chi nhánh Quận 1',     '123 Nguyễn Huệ, Q.1, TP.HCM',     '02812341234'),
  (2, 1, 'Chi nhánh Quận 7',     '456 Nguyễn Thị Thập, Q.7, TP.HCM', '02856785678'),
  (3, 1, 'Chi nhánh Bình Thạnh', '789 Nơ Trang Long, Bình Thạnh',    '02898769876');

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. DINING AREAS  (khu vực bàn)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO dining_areas (id, branch_id, name, code, display_order, active)
VALUES
  (1, 1, 'Khu trong nhà',  'INDOOR',   1, TRUE),
  (2, 1, 'Khu ngoài trời', 'OUTDOOR',  2, TRUE),
  (3, 2, 'Tầng 1',         'FLOOR1',   1, TRUE),
  (4, 2, 'Tầng 2',         'FLOOR2',   2, TRUE),
  (5, 3, 'Khu chính',      'MAIN',     1, TRUE);

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. RESTAURANT TABLES
--    unique: (branch_id, table_number)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO tables (id, branch_id, area_id, table_number, capacity, pos_x, pos_y, width, height, display_order, status)
VALUES
  -- Branch 1 – Quận 1
  (1,  1, 1, 'T01', 4, 1.0,  1.0,  2.0, 2.0, 1,  'AVAILABLE'),
  (2,  1, 1, 'T02', 4, 4.0,  1.0,  2.0, 2.0, 2,  'AVAILABLE'),
  (3,  1, 1, 'T03', 6, 7.0,  1.0,  3.0, 2.0, 3,  'AVAILABLE'),
  (4,  1, 2, 'T04', 2, 1.0,  5.0,  2.0, 2.0, 4,  'AVAILABLE'),
  (5,  1, 2, 'T05', 2, 4.0,  5.0,  2.0, 2.0, 5,  'RESERVED'),
  -- Branch 2 – Quận 7
  (6,  2, 3, 'T01', 4, 1.0,  1.0,  2.0, 2.0, 1,  'AVAILABLE'),
  (7,  2, 3, 'T02', 4, 4.0,  1.0,  2.0, 2.0, 2,  'AVAILABLE'),
  (8,  2, 4, 'T03', 8, 1.0,  1.0,  4.0, 3.0, 3,  'AVAILABLE'),
  -- Branch 3 – Bình Thạnh
  (9,  3, 5, 'T01', 4, 1.0,  1.0,  2.0, 2.0, 1,  'AVAILABLE'),
  (10, 3, 5, 'T02', 4, 4.0,  1.0,  2.0, 2.0, 2,  'AVAILABLE');

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. ROLES  (AuthBootstrapRunner đã seed khi startup – dùng INSERT IGNORE)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO roles (id, name, description, created_at, updated_at)
VALUES
  (1, 'ADMIN',    'Quản trị hệ thống',    NOW(), NOW()),
  (2, 'MANAGER',  'Quản lý chi nhánh',    NOW(), NOW()),
  (3, 'STAFF',    'Nhân viên phục vụ',    NOW(), NOW()),
  (4, 'KITCHEN',  'Nhân viên bếp',        NOW(), NOW()),
  (5, 'CUSTOMER', 'Khách hàng tự đăng ký',NOW(), NOW());

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. USERS + USER_PROFILES
--    Password: GoldenHeart@2026  →  BCrypt hash (cost=12)
--    admin đã được bootstrap tạo – dùng INSERT IGNORE
-- ─────────────────────────────────────────────────────────────────────────────
-- BCrypt hash của "GoldenHeart@2026":
SET @pw = '$2a$12$K3L/Dh2mBJy4GpVfEw.9WOBGJzxI8rFhEiGP/wCKH9L3MidG9oqAS';

INSERT IGNORE INTO users (id, username, password_hash, role_id, status, created_at, updated_at)
VALUES
  -- Admin đã có từ bootstrap, để IGNORE
  (1,  'admin',       @pw, 1, 'ACTIVE', NOW(), NOW()),
  -- Manager
  (2,  'manager_q1',  @pw, 2, 'ACTIVE', NOW(), NOW()),
  (3,  'manager_q7',  @pw, 2, 'ACTIVE', NOW(), NOW()),
  -- Staff branch 1
  (4,  'staff_q1_a',  @pw, 3, 'ACTIVE', NOW(), NOW()),
  (5,  'staff_q1_b',  @pw, 3, 'ACTIVE', NOW(), NOW()),
  -- Staff branch 2
  (6,  'staff_q7_a',  @pw, 3, 'ACTIVE', NOW(), NOW()),
  -- Kitchen branch 1
  (7,  'kitchen_q1',  @pw, 4, 'ACTIVE', NOW(), NOW()),
  -- Kitchen branch 2
  (8,  'kitchen_q7',  @pw, 4, 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO user_profiles
  (user_id, full_name, employee_code, email, active_email, phone, active_phone,
   branch_id, date_of_birth, gender, hire_date, salary, address, created_at, updated_at)
VALUES
  (1,  'System Admin',         'EMP-000', 'admin@goldenheart.com',       'admin@goldenheart.com',       '0900000000', '0900000000', NULL, '1990-01-01', 'male', '2024-01-01', 25000000, 'TP.HCM', NOW(), NOW()),
  (2,  'Nguyễn Văn Minh',     'EMP-001', 'manager.q1@goldenheart.com',  'manager.q1@goldenheart.com',  '0901000001', '0901000001', 1,    '1988-03-15', 'male', '2024-01-15', 18000000, 'Q.1, TP.HCM',  NOW(), NOW()),
  (3,  'Trần Thị Lan',        'EMP-002', 'manager.q7@goldenheart.com',  'manager.q7@goldenheart.com',  '0901000002', '0901000002', 2,    '1990-07-22', 'female','2024-02-01', 18000000, 'Q.7, TP.HCM',  NOW(), NOW()),
  (4,  'Lê Văn Hùng',         'EMP-003', 'staff.q1a@goldenheart.com',   'staff.q1a@goldenheart.com',   '0901000003', '0901000003', 1,    '1999-05-10', 'male', '2024-03-01', 8000000,  'Q.Bình Thạnh', NOW(), NOW()),
  (5,  'Phạm Thị Mai',        'EMP-004', 'staff.q1b@goldenheart.com',   'staff.q1b@goldenheart.com',   '0901000004', '0901000004', 1,    '2000-11-20', 'female','2024-03-15', 8000000,  'Q.Tân Bình',   NOW(), NOW()),
  (6,  'Hoàng Văn Nam',       'EMP-005', 'staff.q7a@goldenheart.com',   'staff.q7a@goldenheart.com',   '0901000005', '0901000005', 2,    '1998-08-08', 'male', '2024-04-01', 8000000,  'Q.7, TP.HCM',  NOW(), NOW()),
  (7,  'Đỗ Thị Thu',          'EMP-006', 'kitchen.q1@goldenheart.com',  'kitchen.q1@goldenheart.com',  '0901000006', '0901000006', 1,    '1995-02-14', 'female','2024-01-20', 10000000, 'Q.3, TP.HCM',  NOW(), NOW()),
  (8,  'Bùi Văn Tâm',         'EMP-007', 'kitchen.q7@goldenheart.com',  'kitchen.q7@goldenheart.com',  '0901000007', '0901000007', 2,    '1993-09-30', 'male', '2024-02-05', 10000000, 'Q.7, TP.HCM',  NOW(), NOW());

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. MEASUREMENT UNITS reference (lấy ID để dùng cho ingredients)
-- ─────────────────────────────────────────────────────────────────────────────
-- Lấy ID động để tránh hardcode ID
-- KG=1, G=2, L=3, ML=4, PCS=5, BOX=6, BUNCH=7 (theo thứ tự insert phía trên)

-- ─────────────────────────────────────────────────────────────────────────────
-- 9. INGREDIENTS
--    unique: name
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO ingredients (id, name, unit_id, unit, description)
VALUES
  (1,  'Thịt bò',           1, 'kg',    'Thịt bò Úc tươi'),
  (2,  'Thịt gà',           1, 'kg',    'Thịt gà ta tươi'),
  (3,  'Cơm',               1, 'kg',    'Gạo nấu cơm'),
  (4,  'Tôm',               1, 'kg',    'Tôm sú tươi'),
  (5,  'Mực',               1, 'kg',    'Mực ống tươi'),
  (6,  'Rau cải',           1, 'kg',    'Rau cải xanh'),
  (7,  'Hành tây',          1, 'kg',    'Hành tây vàng'),
  (8,  'Tỏi',               1, 'kg',    'Tỏi tươi'),
  (9,  'Dầu ăn',            3, 'L',     'Dầu thực vật'),
  (10, 'Nước mắm',          3, 'L',     'Nước mắm nhĩ'),
  (11, 'Phở bò – bánh phở', 1, 'kg',   'Bánh phở tươi'),
  (12, 'Xương bò',          1, 'kg',    'Xương bò hầm nước dùng'),
  (13, 'Hải sản hỗn hợp',   1, 'kg',   'Tôm, mực, nghêu'),
  (14, 'Bột mì',            1, 'kg',    'Bột mì đa dụng'),
  (15, 'Trứng gà',          5, 'pcs',   'Trứng gà tươi'),
  (16, 'Dâu tây',           1, 'kg',    'Dâu tây tươi'),
  (17, 'Kem tươi',          3, 'L',     'Kem tươi làm bánh');

-- ─────────────────────────────────────────────────────────────────────────────
-- 10. INVENTORY  (kho theo chi nhánh)
--     unique: (branch_id, ingredient_id, active_record_key)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO inventory
  (id, branch_id, ingredient_id, active_record_key, quantity, min_stock_level, reorder_level, average_unit_cost, last_receipt_at, created_at, updated_at)
VALUES
  -- Branch 1 – Quận 1
  (1,  1, 1,  'ACTIVE', 50.00, 5.00, 10.00, 280000.00, NOW(), NOW(), NOW()),  -- Thịt bò
  (2,  1, 2,  'ACTIVE', 30.00, 3.00,  6.00,  80000.00, NOW(), NOW(), NOW()),  -- Thịt gà
  (3,  1, 3,  'ACTIVE', 80.00, 10.00,20.00,  25000.00, NOW(), NOW(), NOW()),  -- Cơm
  (4,  1, 4,  'ACTIVE', 20.00, 2.00,  5.00, 180000.00, NOW(), NOW(), NOW()),  -- Tôm
  (5,  1, 5,  'ACTIVE', 15.00, 2.00,  4.00, 120000.00, NOW(), NOW(), NOW()),  -- Mực
  (6,  1, 6,  'ACTIVE', 25.00, 3.00,  6.00,  15000.00, NOW(), NOW(), NOW()),  -- Rau cải
  (7,  1, 7,  'ACTIVE', 20.00, 2.00,  5.00,  20000.00, NOW(), NOW(), NOW()),  -- Hành tây
  (8,  1, 8,  'ACTIVE', 10.00, 1.00,  3.00,  40000.00, NOW(), NOW(), NOW()),  -- Tỏi
  (9,  1, 9,  'ACTIVE', 10.00, 1.00,  3.00,  30000.00, NOW(), NOW(), NOW()),  -- Dầu ăn
  (10, 1, 10, 'ACTIVE',  8.00, 1.00,  2.00,  30000.00, NOW(), NOW(), NOW()),  -- Nước mắm
  (11, 1, 11, 'ACTIVE', 40.00, 5.00, 10.00,  18000.00, NOW(), NOW(), NOW()),  -- Bánh phở
  (12, 1, 12, 'ACTIVE', 60.00, 5.00, 15.00,  50000.00, NOW(), NOW(), NOW()),  -- Xương bò
  (13, 1, 13, 'ACTIVE', 25.00, 3.00,  6.00, 150000.00, NOW(), NOW(), NOW()),  -- Hải sản hỗn hợp
  (14, 1, 14, 'ACTIVE', 30.00, 3.00,  8.00,  18000.00, NOW(), NOW(), NOW()),  -- Bột mì
  (15, 1, 15, 'ACTIVE',100.00,10.00, 20.00,   3500.00, NOW(), NOW(), NOW()),  -- Trứng gà
  (16, 1, 16, 'ACTIVE', 10.00, 1.00,  3.00, 100000.00, NOW(), NOW(), NOW()),  -- Dâu tây
  (17, 1, 17, 'ACTIVE',  5.00, 0.50,  1.00,  80000.00, NOW(), NOW(), NOW()),  -- Kem tươi
  -- Branch 2 – Quận 7
  (18, 2, 1,  'ACTIVE', 40.00, 4.00,  8.00, 280000.00, NOW(), NOW(), NOW()),  -- Thịt bò
  (19, 2, 2,  'ACTIVE', 25.00, 3.00,  6.00,  80000.00, NOW(), NOW(), NOW()),  -- Thịt gà
  (20, 2, 3,  'ACTIVE', 60.00, 8.00, 16.00,  25000.00, NOW(), NOW(), NOW()),  -- Cơm
  (21, 2, 4,  'ACTIVE', 15.00, 2.00,  4.00, 180000.00, NOW(), NOW(), NOW()),  -- Tôm
  (22, 2, 5,  'ACTIVE', 12.00, 1.50,  3.00, 120000.00, NOW(), NOW(), NOW()),  -- Mực
  (23, 2, 6,  'ACTIVE', 20.00, 2.00,  5.00,  15000.00, NOW(), NOW(), NOW()),  -- Rau cải
  (24, 2, 11, 'ACTIVE', 30.00, 4.00,  8.00,  18000.00, NOW(), NOW(), NOW()),  -- Bánh phở
  (25, 2, 12, 'ACTIVE', 50.00, 5.00, 12.00,  50000.00, NOW(), NOW(), NOW());  -- Xương bò

-- ─────────────────────────────────────────────────────────────────────────────
-- 11. CATEGORIES
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO categories (id, name, description)
VALUES
  (1, 'Món Chính',   'Các món ăn chính'),
  (2, 'Khai Vị',     'Các món khai vị'),
  (3, 'Tráng Miệng', 'Các món tráng miệng'),
  (4, 'Đồ Uống',     'Thức uống các loại'),
  (5, 'Cơm – Cháo',  'Cơm, cháo, mì'),
  (6, 'Hải Sản',     'Các món hải sản');

-- ─────────────────────────────────────────────────────────────────────────────
-- 12. MENU ITEMS  (gắn theo branch + category)
--     unique: (branch_id, category_id, name)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO menu_items (id, branch_id, category_id, name, description, price, status)
VALUES
  -- ── Branch 1 – Quận 1 ────────────────────────────────────────────────────
  -- Món Chính
  (1,  1, 1, 'Bò Lúc Lắc',            'Thịt bò xào sốt tiêu đen, khoai tây chiên',   189000, 'AVAILABLE'),
  (2,  1, 1, 'Gà Nướng Mật Ong',      'Gà nướng sốt mật ong, rau sống',              159000, 'AVAILABLE'),
  (3,  1, 1, 'Tôm Sú Hấp Gừng',       'Tôm sú 500g hấp gừng sả',                     220000, 'AVAILABLE'),
  (4,  1, 1, 'Mực Xào Sa Tế',         'Mực ống xào sa tế, hành tây',                 189000, 'AVAILABLE'),
  -- Khai Vị
  (5,  1, 2, 'Chả Giò Hải Sản',       'Chả giò chiên giòn nhân hải sản',              79000, 'AVAILABLE'),
  -- Tráng Miệng
  (6,  1, 3, 'Bánh Dâu Tây',          'Bánh bông lan nhân kem dâu',                   65000, 'AVAILABLE'),
  -- Cơm – Cháo
  (7,  1, 5, 'Phở Bò Tái',            'Phở bò tái, nước dùng xương hầm',              89000, 'AVAILABLE'),
  (8,  1, 5, 'Cơm Gà Xé',             'Cơm trắng + gà xé + canh',                     75000, 'AVAILABLE'),
  -- Hải Sản
  (9,  1, 6, 'Lẩu Hải Sản',           'Lẩu hải sản hỗn hợp cho 2 người',             350000, 'AVAILABLE'),
  (10, 1, 6, 'Cơm Rang Hải Sản',      'Cơm rang tôm mực hành',                        99000, 'AVAILABLE'),
  -- ── Branch 2 – Quận 7 ────────────────────────────────────────────────────
  (11, 2, 1, 'Bò Lúc Lắc',            'Thịt bò xào sốt tiêu đen, khoai tây chiên',   189000, 'AVAILABLE'),
  (12, 2, 1, 'Gà Nướng Mật Ong',      'Gà nướng sốt mật ong, rau sống',              159000, 'AVAILABLE'),
  (13, 2, 5, 'Phở Bò Tái',            'Phở bò tái, nước dùng xương hầm',              89000, 'AVAILABLE'),
  (14, 2, 6, 'Lẩu Hải Sản',           'Lẩu hải sản hỗn hợp cho 2 người',             350000, 'AVAILABLE'),
  (15, 2, 2, 'Chả Giò Hải Sản',       'Chả giò chiên giòn nhân hải sản',              79000, 'AVAILABLE'),
  -- ── Branch 3 – Bình Thạnh ────────────────────────────────────────────────
  (16, 3, 1, 'Bò Lúc Lắc',            'Thịt bò xào sốt tiêu đen, khoai tây chiên',   189000, 'AVAILABLE'),
  (17, 3, 5, 'Cơm Gà Xé',             'Cơm trắng + gà xé + canh',                     75000, 'AVAILABLE');

-- ─────────────────────────────────────────────────────────────────────────────
-- 13. RECIPES  (mỗi menu_item cần ít nhất 1 recipe để kitchen complete được)
--     unique: (menu_item_id, ingredient_id)
--     Quantity = lượng nguyên liệu cho 1 phần ăn
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO recipes (menu_item_id, ingredient_id, quantity)
VALUES
  -- (1) Bò Lúc Lắc – Branch 1
  (1,  1,  0.20),  -- 200g thịt bò
  (1,  7,  0.05),  -- 50g hành tây
  (1,  9,  0.02),  -- 20mL dầu ăn  (unit kg but stored as L – ok vì unit là meta)
  -- (2) Gà Nướng Mật Ong – Branch 1
  (2,  2,  0.30),  -- 300g thịt gà
  (2,  8,  0.02),  -- 20g tỏi
  -- (3) Tôm Sú Hấp Gừng – Branch 1
  (3,  4,  0.50),  -- 500g tôm
  -- (4) Mực Xào Sa Tế – Branch 1
  (4,  5,  0.30),  -- 300g mực
  (4,  7,  0.05),  -- 50g hành tây
  -- (5) Chả Giò Hải Sản – Branch 1
  (5,  13, 0.10),  -- 100g hải sản hỗn hợp
  (5,  14, 0.05),  -- 50g bột mì
  -- (6) Bánh Dâu Tây – Branch 1
  (6,  16, 0.10),  -- 100g dâu tây
  (6,  17, 0.05),  -- 50mL kem tươi
  (6,  14, 0.08),  -- 80g bột mì
  -- (7) Phở Bò Tái – Branch 1
  (7,  11, 0.20),  -- 200g bánh phở
  (7,  1,  0.10),  -- 100g thịt bò tái
  (7,  12, 0.15),  -- 150g xương bò (nước dùng)
  -- (8) Cơm Gà Xé – Branch 1
  (8,  3,  0.20),  -- 200g cơm
  (8,  2,  0.20),  -- 200g thịt gà
  -- (9) Lẩu Hải Sản – Branch 1
  (9,  13, 0.50),  -- 500g hải sản hỗn hợp
  (9,  6,  0.20),  -- 200g rau cải
  -- (10) Cơm Rang Hải Sản – Branch 1
  (10, 3,  0.20),  -- 200g cơm
  (10, 13, 0.15),  -- 150g hải sản
  (10, 15, 1.00),  -- 1 trứng gà
  -- (11) Bò Lúc Lắc – Branch 2
  (11, 1,  0.20),
  (11, 7,  0.05),
  -- (12) Gà Nướng Mật Ong – Branch 2
  (12, 2,  0.30),
  (12, 8,  0.02),
  -- (13) Phở Bò Tái – Branch 2
  (13, 11, 0.20),  -- bánh phở (ingredient id=11, shared ingredient)
  (13, 1,  0.10),  -- thịt bò tái
  -- (14) Lẩu Hải Sản – Branch 2
  (14, 13, 0.50),
  (14, 6,  0.20),
  -- (15) Chả Giò Hải Sản – Branch 2
  (15, 13, 0.10),
  (15, 14, 0.05),
  -- (16) Bò Lúc Lắc – Branch 3, dùng ingredient chung
  (16, 1,  0.20),
  (16, 7,  0.05),
  -- (17) Cơm Gà Xé – Branch 3
  (17, 3,  0.20),
  (17, 2,  0.20);

-- ─────────────────────────────────────────────────────────────────────────────
-- 14. CUSTOMERS
-- ─────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO customers
  (id, name, phone, email, active_phone, active_email, address, loyalty_points, last_visit_at, created_at, updated_at)
VALUES
  (1, 'Nguyễn Minh Khoa', '0912345678', 'khoa.nguyen@email.com', '0912345678', 'khoa.nguyen@email.com', 'Q.1, TP.HCM',  0, NULL, NOW(), NOW()),
  (2, 'Trần Thị Hương',   '0923456789', 'huong.tran@email.com',  '0923456789', 'huong.tran@email.com',  'Q.7, TP.HCM',  0, NULL, NOW(), NOW()),
  (3, 'Lê Quốc Bảo',     '0934567890', 'bao.le@email.com',      '0934567890', 'bao.le@email.com',      'Q.Bình Thạnh', 0, NULL, NOW(), NOW()),
  (4, 'Phạm Thị Diễm',   '0945678901', 'diem.pham@email.com',   '0945678901', 'diem.pham@email.com',   'Q.3, TP.HCM',  0, NULL, NOW(), NOW()),
  (5, 'Hoàng Anh Tuấn',  '0956789012', 'tuan.hoang@email.com',  '0956789012', 'tuan.hoang@email.com',  'Q.Tân Bình',   0, NULL, NOW(), NOW());

-- ─────────────────────────────────────────────────────────────────────────────
-- 15. WORK SHIFTS  (operations module – entity đã có, bổ sung data để test sau)
-- ─────────────────────────────────────────────────────────────────────────────
-- Để trống vì operations module chưa có controller/service

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- SUMMARY IDs để dùng trong Postman:
-- ─────────────────────────────────────────────────────────────────────────────
-- Roles:      ADMIN=1, MANAGER=2, STAFF=3, KITCHEN=4, CUSTOMER=5
-- Branches:   Q1=1, Q7=2, BinhThanh=3
-- Tables Q1:  T01=1, T02=2, T03=3, T04=4, T05=5(RESERVED)
-- Tables Q7:  T01=6, T02=7, T03=8
-- Users:      admin=1, manager_q1=2, manager_q7=3, staff_q1_a=4,
--             staff_q1_b=5, staff_q7_a=6, kitchen_q1=7, kitchen_q7=8
-- Categories: Món Chính=1, Khai Vị=2, Tráng Miệng=3, Đồ Uống=4, Cơm=5, Hải Sản=6
-- MenuItems Q1: 1–10,  MenuItems Q7: 11–15, MenuItems Q3: 16–17
-- Customers:  1–5
-- Inventory Q1: id 1–17, Q7: 18–25
-- =============================================================================
