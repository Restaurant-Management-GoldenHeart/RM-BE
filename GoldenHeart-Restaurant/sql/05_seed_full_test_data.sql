-- =============================================================================
-- GoldenHeart Restaurant - Dữ liệu seed đầy đủ cho kiểm thử
-- Tệp: 05_seed_full_test_data.sql
--   1. Đơn vị tính
--   2. Nhà hàng
--   3. Chi nhánh
--   4. Khu vực bàn
--   5. Bàn ăn
--   6. Vai trò (bootstrap đã tạo, vẫn dùng INSERT IGNORE để an toàn)
--   7. Người dùng + hồ sơ người dùng
--   8. Nguyên liệu
--   9. Tồn kho
--  10. Danh mục
--  11. Món ăn + công thức
--  12. Khách hàng
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. ĐƠN VỊ TÍNH
-- Ràng buộc duy nhất: active_code, active_symbol
INSERT IGNORE INTO measurement_units (code, name, symbol, active_code, active_symbol, description, created_at, updated_at)
VALUES
  ('KG',    'Kilôgam',     'kg',    'KG',    'kg',    'Đơn vị khối lượng',       NOW(), NOW()),
  ('G',     'Gam',         'g',     'G',     'g',     'Đơn vị khối lượng nhỏ',   NOW(), NOW()),
  ('L',     'Lít',         'L',     'L',     'L',     'Đơn vị thể tích',         NOW(), NOW()),
  ('ML',    'Mililít',     'mL',    'ML',    'mL',    'Đơn vị thể tích nhỏ',     NOW(), NOW()),
  ('PCS',   'Cái/Chiếc',   'pcs',   'PCS',   'pcs',   'Đơn vị đếm',              NOW(), NOW()),
  ('BOX',   'Hộp',         'box',   'BOX',   'box',   'Đơn vị đóng gói',         NOW(), NOW()),
  ('BUNCH', 'Bó',          'bunch', 'BUNCH', 'bunch', 'Đơn vị bó rau củ',        NOW(), NOW());

-- 2. NHÀ HÀNG
INSERT IGNORE INTO restaurants (id, name, address, phone)
VALUES (1, 'Nhà hàng GoldenHeart', '123 Golden Heart Center, TP.HCM', '02899998888');

-- 3. CHI NHÁNH
INSERT IGNORE INTO branches (id, restaurant_id, name, address, phone)
VALUES
  (1, 1, 'Chi nhánh Quận 1',     '123 Nguyễn Huệ, Quận 1, TP.HCM',           '02812341234'),
  (2, 1, 'Chi nhánh Quận 7',     '456 Nguyễn Thị Thập, Quận 7, TP.HCM',      '02856785678'),
  (3, 1, 'Chi nhánh Bình Thạnh', '789 Nơ Trang Long, Bình Thạnh, TP.HCM',    '02898769876');

-- 4. KHU VỰC BÀN
INSERT IGNORE INTO dining_areas (id, branch_id, name, code, display_order, active)
VALUES
  (1, 1, 'Khu trong nhà',   'INDOOR',  1, TRUE),
  (2, 1, 'Khu ngoài trời',  'OUTDOOR', 2, TRUE),
  (3, 2, 'Tầng 1',          'FLOOR1',  1, TRUE),
  (4, 2, 'Tầng 2',          'FLOOR2',  2, TRUE),
  (5, 3, 'Khu chính',       'MAIN',    1, TRUE);

-- 5. BÀN ĂN
-- Ràng buộc duy nhất: (branch_id, table_number)
INSERT IGNORE INTO tables (id, branch_id, area_id, table_number, capacity, pos_x, pos_y, width, height, display_order, status)
VALUES
  -- Chi nhánh Quận 1
  (1,  1, 1, 'T01', 4, 1.0, 1.0, 2.0, 2.0, 1, 'AVAILABLE'),
  (2,  1, 1, 'T02', 4, 4.0, 1.0, 2.0, 2.0, 2, 'AVAILABLE'),
  (3,  1, 1, 'T03', 6, 7.0, 1.0, 3.0, 2.0, 3, 'AVAILABLE'),
  (4,  1, 2, 'T04', 2, 1.0, 5.0, 2.0, 2.0, 4, 'AVAILABLE'),
  (5,  1, 2, 'T05', 2, 4.0, 5.0, 2.0, 2.0, 5, 'RESERVED'),
  (11, 1, 1, 'T06', 4, 10.0, 1.0, 2.0, 2.0, 6, 'AVAILABLE'),
  (12, 1, 1, 'T07', 4, 13.0, 1.0, 2.0, 2.0, 7, 'AVAILABLE'),
  -- Chi nhánh Quận 7
  (6,  2, 3, 'T01', 4, 1.0, 1.0, 2.0, 2.0, 1, 'AVAILABLE'),
  (7,  2, 3, 'T02', 4, 4.0, 1.0, 2.0, 2.0, 2, 'AVAILABLE'),
  (8,  2, 4, 'T03', 8, 1.0, 1.0, 4.0, 3.0, 3, 'AVAILABLE'),
  -- Chi nhánh Bình Thạnh
  (9,  3, 5, 'T01', 4, 1.0, 1.0, 2.0, 2.0, 1, 'AVAILABLE'),
  (10, 3, 5, 'T02', 4, 4.0, 1.0, 2.0, 2.0, 2, 'AVAILABLE');

-- 6. VAI TRÒ
INSERT IGNORE INTO roles (id, name, description, created_at, updated_at)
VALUES
  (1, 'ADMIN',    'Quản trị hệ thống',            NOW(), NOW()),
  (2, 'MANAGER',  'Quản lý chi nhánh',            NOW(), NOW()),
  (3, 'STAFF',    'Nhân viên phục vụ',            NOW(), NOW()),
  (4, 'KITCHEN',  'Nhân viên bếp',                NOW(), NOW()),
  (5, 'CUSTOMER', 'Khách hàng tự đăng ký',        NOW(), NOW());

-- 7. NGƯỜI DÙNG + HỒ SƠ NGƯỜI DÙNG
-- Mật khẩu: GoldenHeart@2026 -> BCrypt hash (cost=12)
-- admin đã được bootstrap tạo, vẫn dùng INSERT IGNORE
-- Tài khoản test luồng mật khẩu:
--   username = staff_q1_b
--   password = GoldenHeart@2026
--   email    = staff.q1b@goldenheart.com
--   phone    = 0901000004
-- BCrypt hash của "GoldenHeart@2026" (đã xác thực với Spring BCryptPasswordEncoder cost=12)
SET @pw = '$2a$12$EXb/TNXubsjiFf6pYAFNi.z88B.QuwDGz69Nt/OUJGmp9hcBJaLqC';

INSERT IGNORE INTO users (id, username, password_hash, role_id, status, created_at, updated_at)
VALUES
  -- Quản trị
  (1, 'admin',      @pw, 1, 'ACTIVE', NOW(), NOW()),
  -- Quản lý
  (2, 'manager_q1', @pw, 2, 'ACTIVE', NOW(), NOW()),
  (3, 'manager_q7', @pw, 2, 'ACTIVE', NOW(), NOW()),
  -- Nhân viên phục vụ chi nhánh 1
  (4, 'staff_q1_a', @pw, 3, 'ACTIVE', NOW(), NOW()),
  (5, 'staff_q1_b', @pw, 3, 'ACTIVE', NOW(), NOW()),
  -- Nhân viên phục vụ chi nhánh 2
  (6, 'staff_q7_a', @pw, 3, 'ACTIVE', NOW(), NOW()),
  -- Nhân viên bếp chi nhánh 1
  (7, 'kitchen_q1', @pw, 4, 'ACTIVE', NOW(), NOW()),
  -- Nhân viên bếp chi nhánh 2
  (8, 'kitchen_q7', @pw, 4, 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO user_profiles
  (user_id, full_name, employee_code, email, active_email, phone, active_phone,
   branch_id, date_of_birth, gender, hire_date, salary, address, created_at, updated_at)
VALUES
  (1, 'Quản trị hệ thống',   'EMP-000', 'admin@goldenheart.com',      'admin@goldenheart.com',      '0900000000', '0900000000', NULL, '1990-01-01', 'male',   '2024-01-01', 25000000, 'TP.HCM',             NOW(), NOW()),
  (2, 'Nguyễn Văn Minh',     'EMP-001', 'manager.q1@goldenheart.com', 'manager.q1@goldenheart.com', '0901000001', '0901000001', 1,    '1988-03-15', 'male',   '2024-01-15', 18000000, 'Quận 1, TP.HCM',     NOW(), NOW()),
  (3, 'Trần Thị Lan',        'EMP-002', 'manager.q7@goldenheart.com', 'manager.q7@goldenheart.com', '0901000002', '0901000002', 2,    '1990-07-22', 'female', '2024-02-01', 18000000, 'Quận 7, TP.HCM',     NOW(), NOW()),
  (4, 'Lê Văn Hùng',         'EMP-003', 'staff.q1a@goldenheart.com',  'staff.q1a@goldenheart.com',  '0901000003', '0901000003', 1,    '1999-05-10', 'male',   '2024-03-01',  8000000, 'Quận Bình Thạnh',    NOW(), NOW()),
  (5, 'Phạm Thị Mai',        'EMP-004', 'staff.q1b@goldenheart.com',  'staff.q1b@goldenheart.com',  '0901000004', '0901000004', 1,    '2000-11-20', 'female', '2024-03-15',  8000000, 'Quận Tân Bình',      NOW(), NOW()),
  (6, 'Hoàng Văn Nam',       'EMP-005', 'staff.q7a@goldenheart.com',  'staff.q7a@goldenheart.com',  '0901000005', '0901000005', 2,    '1998-08-08', 'male',   '2024-04-01',  8000000, 'Quận 7, TP.HCM',     NOW(), NOW()),
  (7, 'Đỗ Thị Thu',          'EMP-006', 'kitchen.q1@goldenheart.com', 'kitchen.q1@goldenheart.com', '0901000006', '0901000006', 1,    '1995-02-14', 'female', '2024-01-20', 10000000, 'Quận 3, TP.HCM',     NOW(), NOW()),
  (8, 'Bùi Văn Tâm',         'EMP-007', 'kitchen.q7@goldenheart.com', 'kitchen.q7@goldenheart.com', '0901000007', '0901000007', 2,    '1993-09-30', 'male',   '2024-02-05', 10000000, 'Quận 7, TP.HCM',     NOW(), NOW());

-- 8. THAM CHIẾU ĐƠN VỊ TÍNH
-- Ghi chú ID cố định
-- KG=1, G=2, L=3, ML=4, PCS=5, BOX=6, BUNCH=7

-- 9. NGUYÊN LIỆU
-- Ràng buộc duy nhất: name
INSERT IGNORE INTO ingredients (id, name, unit_id, unit, description)
VALUES
  (1,  'Thịt bò',              1, 'kg',  'Thịt bò Úc tươi'),
  (2,  'Thịt gà',              1, 'kg',  'Thịt gà ta tươi'),
  (3,  'Cơm',                  1, 'kg',  'Gạo nấu cơm'),
  (4,  'Tôm',                  1, 'kg',  'Tôm sú tươi'),
  (5,  'Mực',                  1, 'kg',  'Mực ống tươi'),
  (6,  'Rau cải',              1, 'kg',  'Rau cải xanh'),
  (7,  'Hành tây',             1, 'kg',  'Hành tây vàng'),
  (8,  'Tỏi',                  1, 'kg',  'Tỏi tươi'),
  (9,  'Dầu ăn',               3, 'L',   'Dầu thực vật'),
  (10, 'Nước mắm',             3, 'L',   'Nước mắm nhĩ'),
  (11, 'Phở bò - bánh phở',    1, 'kg',  'Bánh phở tươi'),
  (12, 'Xương bò',             1, 'kg',  'Xương bò hầm nước dùng'),
  (13, 'Hải sản hỗn hợp',      1, 'kg',  'Tôm, mực, nghêu'),
  (14, 'Bột mì',               1, 'kg',  'Bột mì đa dụng'),
  (15, 'Trứng gà',             5, 'pcs', 'Trứng gà tươi'),
  (16, 'Dâu tây',              1, 'kg',  'Dâu tây tươi'),
  (17, 'Kem tươi',             3, 'L',   'Kem tươi làm bánh');

-- 10. TỒN KHO THEO CHI NHÁNH
-- Ràng buộc duy nhất: (branch_id, ingredient_id, active_record_key)
INSERT IGNORE INTO inventory
  (id, branch_id, ingredient_id, active_record_key, quantity, min_stock_level, reorder_level, average_unit_cost, last_receipt_at, created_at, updated_at)
VALUES
  -- Chi nhánh Quận 1
  (1,  1, 1,  'ACTIVE', 50.00,  5.00, 10.00, 280000.00, NOW(), NOW(), NOW()), -- Thịt bò
  (2,  1, 2,  'ACTIVE', 30.00,  3.00,  6.00,  80000.00, NOW(), NOW(), NOW()), -- Thịt gà
  (3,  1, 3,  'ACTIVE', 80.00, 10.00, 20.00,  25000.00, NOW(), NOW(), NOW()), -- Cơm
  (4,  1, 4,  'ACTIVE', 20.00,  2.00,  5.00, 180000.00, NOW(), NOW(), NOW()), -- Tôm
  (5,  1, 5,  'ACTIVE', 15.00,  2.00,  4.00, 120000.00, NOW(), NOW(), NOW()), -- Mực
  (6,  1, 6,  'ACTIVE', 25.00,  3.00,  6.00,  15000.00, NOW(), NOW(), NOW()), -- Rau cải
  (7,  1, 7,  'ACTIVE', 20.00,  2.00,  5.00,  20000.00, NOW(), NOW(), NOW()), -- Hành tây
  (8,  1, 8,  'ACTIVE', 10.00,  1.00,  3.00,  40000.00, NOW(), NOW(), NOW()), -- Tỏi
  (9,  1, 9,  'ACTIVE', 10.00,  1.00,  3.00,  30000.00, NOW(), NOW(), NOW()), -- Dầu ăn
  (10, 1, 10, 'ACTIVE',  8.00,  1.00,  2.00,  30000.00, NOW(), NOW(), NOW()), -- Nước mắm
  (11, 1, 11, 'ACTIVE', 40.00,  5.00, 10.00,  18000.00, NOW(), NOW(), NOW()), -- Bánh phở
  (12, 1, 12, 'ACTIVE', 60.00,  5.00, 15.00,  50000.00, NOW(), NOW(), NOW()), -- Xương bò
  (13, 1, 13, 'ACTIVE', 25.00,  3.00,  6.00, 150000.00, NOW(), NOW(), NOW()), -- Hải sản hỗn hợp
  (14, 1, 14, 'ACTIVE', 30.00,  3.00,  8.00,  18000.00, NOW(), NOW(), NOW()), -- Bột mì
  (15, 1, 15, 'ACTIVE',100.00, 10.00, 20.00,   3500.00, NOW(), NOW(), NOW()), -- Trứng gà
  (16, 1, 16, 'ACTIVE', 10.00,  1.00,  3.00, 100000.00, NOW(), NOW(), NOW()), -- Dâu tây
  (17, 1, 17, 'ACTIVE',  5.00,  0.50,  1.00,  80000.00, NOW(), NOW(), NOW()), -- Kem tươi
  -- Chi nhánh Quận 7
  (18, 2, 1,  'ACTIVE', 40.00,  4.00,  8.00, 280000.00, NOW(), NOW(), NOW()), -- Thịt bò
  (19, 2, 2,  'ACTIVE', 25.00,  3.00,  6.00,  80000.00, NOW(), NOW(), NOW()), -- Thịt gà
  (20, 2, 3,  'ACTIVE', 60.00,  8.00, 16.00,  25000.00, NOW(), NOW(), NOW()), -- Cơm
  (21, 2, 4,  'ACTIVE', 15.00,  2.00,  4.00, 180000.00, NOW(), NOW(), NOW()), -- Tôm
  (22, 2, 5,  'ACTIVE', 12.00,  1.50,  3.00, 120000.00, NOW(), NOW(), NOW()), -- Mực
  (23, 2, 6,  'ACTIVE', 20.00,  2.00,  5.00,  15000.00, NOW(), NOW(), NOW()), -- Rau cải
  (24, 2, 11, 'ACTIVE', 30.00,  4.00,  8.00,  18000.00, NOW(), NOW(), NOW()), -- Bánh phở
  (25, 2, 12, 'ACTIVE', 50.00,  5.00, 12.00,  50000.00, NOW(), NOW(), NOW()); -- Xương bò

-- 11. DANH MỤC
INSERT IGNORE INTO categories (id, name, description)
VALUES
  (1, 'Món chính',    'Các món ăn chính'),
  (2, 'Khai vị',      'Các món khai vị'),
  (3, 'Tráng miệng',  'Các món tráng miệng'),
  (4, 'Đồ uống',      'Thức uống các loại'),
  (5, 'Cơm - Cháo',   'Các món cơm, cháo, mì'),
  (6, 'Hải sản',      'Các món hải sản');

-- 12. MÓN ĂN (gắn theo chi nhánh + danh mục)
-- Ràng buộc duy nhất: (branch_id, category_id, name)
INSERT IGNORE INTO menu_items (id, branch_id, category_id, name, description, price, status)
VALUES
  -- Món chính
  (1,  1, 1, 'Bò lúc lắc',          'Thịt bò xào sốt tiêu đen, ăn kèm khoai tây chiên',            189000, 'AVAILABLE'),
  (2,  1, 1, 'Gà nướng mật ong',    'Gà nướng sốt mật ong, ăn kèm rau sống',                       159000, 'AVAILABLE'),
  (3,  1, 1, 'Tôm sú hấp gừng',     'Tôm sú 500g hấp gừng sả',                                     220000, 'AVAILABLE'),
  (4,  1, 1, 'Mực xào sa tế',       'Mực ống xào sa tế cùng hành tây',                             189000, 'AVAILABLE'),
  -- Khai vị
  (5,  1, 2, 'Chả giò hải sản',     'Chả giò chiên giòn nhân hải sản',                              79000, 'AVAILABLE'),
  -- Tráng miệng
  (6,  1, 3, 'Bánh dâu tây',        'Bánh bông lan nhân kem dâu',                                   65000, 'AVAILABLE'),
  -- Cơm - Cháo
  (7,  1, 5, 'Phở bò tái',          'Phở bò tái, nước dùng xương hầm',                              89000, 'AVAILABLE'),
  (8,  1, 5, 'Cơm gà xé',           'Cơm trắng ăn kèm gà xé và canh',                               75000, 'AVAILABLE'),
  -- Hải sản
  (9,  1, 6, 'Lẩu hải sản',         'Lẩu hải sản hỗn hợp cho 2 người',                             350000, 'AVAILABLE'),
  (10, 1, 6, 'Cơm rang hải sản',    'Cơm rang tôm mực và hành',                                     99000, 'AVAILABLE'),
  -- Chi nhánh Quận 7
  (11, 2, 1, 'Bò lúc lắc',          'Thịt bò xào sốt tiêu đen, ăn kèm khoai tây chiên',            189000, 'AVAILABLE'),
  (12, 2, 1, 'Gà nướng mật ong',    'Gà nướng sốt mật ong, ăn kèm rau sống',                       159000, 'AVAILABLE'),
  (13, 2, 5, 'Phở bò tái',          'Phở bò tái, nước dùng xương hầm',                              89000, 'AVAILABLE'),
  (14, 2, 6, 'Lẩu hải sản',         'Lẩu hải sản hỗn hợp cho 2 người',                             350000, 'AVAILABLE'),
  (15, 2, 2, 'Chả giò hải sản',     'Chả giò chiên giòn nhân hải sản',                              79000, 'AVAILABLE'),
  -- Chi nhánh Bình Thạnh
  (16, 3, 1, 'Bò lúc lắc',          'Thịt bò xào sốt tiêu đen, ăn kèm khoai tây chiên',            189000, 'AVAILABLE'),
  (17, 3, 5, 'Cơm gà xé',           'Cơm trắng ăn kèm gà xé và canh',                               75000, 'AVAILABLE');

-- 13. CÔNG THỨC (mỗi món cần ít nhất 1 công thức để bếp xử lý được)
-- Ràng buộc duy nhất: (menu_item_id, ingredient_id)
-- quantity = lượng nguyên liệu cho 1 phần ăn
INSERT IGNORE INTO recipes (menu_item_id, ingredient_id, quantity)
VALUES
  -- (1) Bò lúc lắc - chi nhánh Quận 1
  (1,  1,  0.20), -- 200g thịt bò
  (1,  7,  0.05), -- 50g hành tây
  (1,  9,  0.02), -- 20mL dầu ăn
  -- (2) Gà nướng mật ong - chi nhánh Quận 1
  (2,  2,  0.30), -- 300g thịt gà
  (2,  8,  0.02), -- 20g tỏi
  -- (3) Tôm sú hấp gừng - chi nhánh Quận 1
  (3,  4,  0.50), -- 500g tôm
  -- (4) Mực xào sa tế - chi nhánh Quận 1
  (4,  5,  0.30), -- 300g mực
  (4,  7,  0.05), -- 50g hành tây
  -- (5) Chả giò hải sản - chi nhánh Quận 1
  (5,  13, 0.10), -- 100g hải sản hỗn hợp
  (5,  14, 0.05), -- 50g bột mì
  -- (6) Bánh dâu tây - chi nhánh Quận 1
  (6,  16, 0.10), -- 100g dâu tây
  (6,  17, 0.05), -- 50mL kem tươi
  (6,  14, 0.08), -- 80g bột mì
  -- (7) Phở bò tái - chi nhánh Quận 1
  (7,  11, 0.20), -- 200g bánh phở
  (7,  1,  0.10), -- 100g thịt bò tái
  (7,  12, 0.15), -- 150g xương bò cho nước dùng
  -- (8) Cơm gà xé - chi nhánh Quận 1
  (8,  3,  0.20), -- 200g cơm
  (8,  2,  0.20), -- 200g thịt gà
  -- (9) Lẩu hải sản - chi nhánh Quận 1
  (9,  13, 0.50), -- 500g hải sản hỗn hợp
  (9,  6,  0.20), -- 200g rau cải
  -- (10) Cơm rang hải sản - chi nhánh Quận 1
  (10, 3,  0.20), -- 200g cơm
  (10, 13, 0.15), -- 150g hải sản
  (10, 15, 1.00), -- 1 trứng gà
  -- (11) Bò lúc lắc - chi nhánh Quận 7
  (11, 1,  0.20),
  (11, 7,  0.05),
  -- (12) Gà nướng mật ong - chi nhánh Quận 7
  (12, 2,  0.30),
  (12, 8,  0.02),
  -- (13) Phở bò tái - chi nhánh Quận 7
  (13, 11, 0.20), -- bánh phở
  (13, 1,  0.10), -- thịt bò tái
  -- (14) Lẩu hải sản - chi nhánh Quận 7
  (14, 13, 0.50),
  (14, 6,  0.20),
  -- (15) Chả giò hải sản - chi nhánh Quận 7
  (15, 13, 0.10),
  (15, 14, 0.05),
  -- (16) Bò lúc lắc - chi nhánh Bình Thạnh
  (16, 1,  0.20),
  (16, 7,  0.05),
  -- (17) Cơm gà xé - chi nhánh Bình Thạnh
  (17, 3,  0.20),
  (17, 2,  0.20);

-- 14. KHÁCH HÀNG
INSERT IGNORE INTO customers
  (id, name, phone, email, active_phone, active_email, address, loyalty_points, last_visit_at, created_at, updated_at)
VALUES
  (1, 'Nguyễn Minh Khoa', '0912345678', 'khoa.nguyen@email.com', '0912345678', 'khoa.nguyen@email.com', 'Quận 1, TP.HCM',        80,   '2026-04-10 09:00:00', NOW(), NOW()),
  (2, 'Trần Thị Hương',   '0923456789', 'huong.tran@email.com',  '0923456789', 'huong.tran@email.com',  'Quận 7, TP.HCM',       150,   '2026-04-11 18:30:00', NOW(), NOW()),
  (3, 'Lê Quốc Bảo',      '0934567890', 'bao.le@email.com',      '0934567890', 'bao.le@email.com',      'Bình Thạnh, TP.HCM',   210,   '2026-04-12 12:15:00', NOW(), NOW()),
  (4, 'Phạm Thị Diễm',    '0945678901', 'diem.pham@email.com',   '0945678901', 'diem.pham@email.com',   'Quận 3, TP.HCM',       542,   '2026-04-16 19:10:00', NOW(), NOW()),
  (5, 'Hoàng Anh Tuấn',   '0956789012', 'tuan.hoang@email.com',  '0956789012', 'tuan.hoang@email.com',  'Quận Tân Bình, TP.HCM',1005,  '2026-04-13 20:45:00', NOW(), NOW());

-- 14.1 HẠNG KHÁCH HÀNG (cấu hình loyalty)
INSERT IGNORE INTO customer_tiers
  (id, code, name, min_points, discount_rate, active, note, created_at, updated_at)
VALUES
  (1, 'BRONZE',   'Hạng Đồng',      100, 1.00, TRUE, 'Hạng tích điểm mặc định', NOW(), NOW()),
  (2, 'SILVER',   'Hạng Bạc',       200, 2.00, TRUE, 'Hạng tích điểm mặc định', NOW(), NOW()),
  (3, 'GOLD',     'Hạng Vàng',      500, 4.00, TRUE, 'Hạng tích điểm mặc định', NOW(), NOW()),
  (4, 'PLATINUM', 'Hạng Bạch kim',  800, 6.00, TRUE, 'Hạng tích điểm mặc định', NOW(), NOW()),
  (5, 'DIAMOND',  'Hạng Kim cương', 1000, 10.00, TRUE, 'Hạng tích điểm mặc định', NOW(), NOW());

-- 15. CA LÀM VIỆC
-- Module operations hiện đã có entity, có thể bổ sung thêm seed sau nếu cần

-- =============================================================================
-- 16. DỮ LIỆU NGHIỆP VỤ DÙNG CHO POSTMAN KIỂM THỬ ĐẦU-CUỐI
-- =============================================================================

-- 16.1 Trạng thái biên cho món ăn
UPDATE menu_items
SET status = 'OUT_OF_STOCK'
WHERE id = 10;

-- 16.2 Điều chỉnh tồn kho để tạo các kịch bản bếp có thể test lại ổn định
UPDATE inventory
SET quantity = CASE id
        WHEN 1 THEN 49.90
        WHEN 2 THEN 29.60
        WHEN 3 THEN 79.60
        WHEN 11 THEN 39.80
        WHEN 12 THEN 59.85
        WHEN 13 THEN 0.30
        ELSE quantity
    END,
    min_stock_level = CASE id
        WHEN 13 THEN 1.00
        ELSE min_stock_level
    END,
    reorder_level = CASE id
        WHEN 13 THEN 2.00
        ELSE reorder_level
    END
WHERE id IN (1, 2, 3, 11, 12, 13);

-- 16.3 Đơn hàng seed bao phủ đủ trạng thái: đang xử lý, hoàn tất, đã thanh toán,
--      đã hủy, thanh toán một phần và cặp order độc lập để test gộp bàn
INSERT IGNORE INTO orders (id, branch_id, table_id, customer_id, created_by, status, created_at, closed_at)
VALUES
  (1, 1, 2,    1, 4, 'PROCESSING', '2026-04-17 10:00:00', NULL),
  (2, 1, 3,    2, 4, 'COMPLETED',  '2026-04-17 11:00:00', NULL),
  (3, 1, 4,    4, 5, 'COMPLETED',  '2026-04-16 18:00:00', '2026-04-16 19:10:00'),
  (4, 2, NULL, 4, 6, 'CANCELLED',  '2026-04-15 12:30:00', '2026-04-15 12:45:00'),
  (5, 2, 7,    5, 6, 'COMPLETED',  '2026-04-17 09:15:00', NULL),
  (6, 1, 11,   1, 4, 'PENDING',    '2026-04-17 09:35:00', NULL),
  (7, 1, 12,   2, 4, 'PENDING',    '2026-04-17 09:40:00', NULL);

-- 16.4 Món trong đơn với trạng thái bếp thực tế, ghi chú rõ ràng,
--      lý do hủy và dữ liệu riêng cho kịch bản gộp bàn độc lập
INSERT IGNORE INTO order_items (id, order_id, menu_item_id, quantity, price, status, note)
VALUES
  (1, 1, 7,  1,  89000.00, 'PROCESSING',    'Không hành'),
  (2, 1, 9,  1, 350000.00, 'WAITING_STOCK', 'Thiếu nguyên liệu: Hải sản hỗn hợp (cần 0.5, còn 0.3)'),
  (3, 2, 1,  1, 189000.00, 'SERVED',        NULL),
  (4, 2, 5,  2,  79000.00, 'SERVED',        'Thêm rau sống'),
  (5, 3, 8,  2,  75000.00, 'SERVED',        NULL),
  (6, 3, 6,  1,  65000.00, 'SERVED',        NULL),
  (7, 4, 13, 1,  89000.00, 'CANCELLED',     'Bếp hủy món: Khách rời đi trước khi chế biến'),
  (8, 5, 11, 1, 189000.00, 'SERVED',        NULL),
  (9, 5, 15, 2,  79000.00, 'SERVED',        NULL),
  (10, 6, 7, 1,  89000.00, 'PENDING',       'Bàn nguồn cho luồng gộp bàn độc lập'),
  (11, 7, 1, 1, 189000.00, 'PENDING',       'Bàn đích cho luồng gộp bàn độc lập'),
  (12, 6, 5, 2,  79000.00, 'PENDING',       'Món bổ sung ở bàn nguồn để kiểm thử cộng dồn subtotal sau khi gộp'),
  (13, 7, 8, 1,  75000.00, 'PENDING',       'Món bổ sung ở bàn đích để kiểm thử giữ nguyên món gốc sau khi gộp');

-- 16.5 Hóa đơn, thanh toán và dấu vết loyalty cho luồng doanh thu/thu ngân
INSERT IGNORE INTO bills
  (id, order_id, subtotal, tax, discount, loyalty_discount, total, cost_of_goods_sold, gross_profit, applied_customer_tier_id, status)
VALUES
  (1, 3, 215000.00, 17200.00,  6450.00,  6450.00, 225750.00,  62000.00, 146550.00, 3, 'PAID'),
  (2, 5, 347000.00, 27760.00, 17350.00, 17350.00, 357410.00, 105000.00, 224650.00, 5, 'PARTIAL');

INSERT IGNORE INTO payments (id, bill_id, amount, method, paid_at)
VALUES
  (1, 1, 225750.00, 'CASH',           '2026-04-16 19:10:00'),
  (2, 2, 200000.00, 'CREDIT_CARD',    '2026-04-17 10:30:00'),
  (3, 2,  50000.00, 'E_WALLET',       '2026-04-17 10:45:00'),
  (4, 2,  20000.00, 'BANK_TRANSFER',  '2026-04-17 11:00:00');

INSERT IGNORE INTO customer_loyalty_transactions
  (id, customer_id, bill_id, type, points_delta, points_before, points_after, description, created_at)
VALUES
  (1, 1, NULL, 'ADJUSTMENT_IN',   80,    0,   80,  'Thiết lập điểm tích lũy ban đầu cho dữ liệu mẫu',                '2026-04-10 09:00:00'),
  (2, 2, NULL, 'ADJUSTMENT_IN',  150,    0,  150,  'Thiết lập điểm tích lũy ban đầu cho dữ liệu mẫu',                '2026-04-11 18:30:00'),
  (3, 3, NULL, 'ADJUSTMENT_IN',  210,    0,  210,  'Thiết lập điểm tích lũy ban đầu cho dữ liệu mẫu',                '2026-04-12 12:15:00'),
  (4, 4, NULL, 'ADJUSTMENT_IN',  520,    0,  520,  'Thiết lập điểm tích lũy trước bill seed đã thanh toán',          '2026-04-15 17:00:00'),
  (5, 4,    1, 'EARN',            22,  520,  542,  'Điểm nhận được từ bill seed #1 đã thanh toán',                  '2026-04-16 19:10:00'),
  (6, 5, NULL, 'ADJUSTMENT_IN', 1005,    0, 1005,  'Thiết lập điểm tích lũy ban đầu cho khách hạng Kim cương mẫu',  '2026-04-13 20:45:00');

-- 16.6 Biến động kho lịch sử dùng cho lợi nhuận gộp và luồng bếp
INSERT IGNORE INTO stock_movements (
  id, branch_id, ingredient_id, order_id, order_item_id, created_by,
  movement_type, quantity_change, balance_after, unit_cost, total_cost, occurred_at, note
)
VALUES
  (1, 1, 11, 1, 1, 7, 'SALE_OUT', -0.20, 39.80,  18000.00,  3600.00, '2026-04-17 10:05:00', 'Trừ kho khi món seed #1 chuyển sang trạng thái PROCESSING'),
  (2, 1,  1, 1, 1, 7, 'SALE_OUT', -0.10, 49.90, 280000.00, 28000.00, '2026-04-17 10:05:00', 'Trừ kho khi món seed #1 chuyển sang trạng thái PROCESSING'),
  (3, 1, 12, 1, 1, 7, 'SALE_OUT', -0.15, 59.85,  50000.00,  7500.00, '2026-04-17 10:05:00', 'Trừ kho khi món seed #1 chuyển sang trạng thái PROCESSING'),
  (4, 1,  3, 3, 5, 7, 'SALE_OUT', -0.40, 79.60,  25000.00, 10000.00, '2026-04-16 18:10:00', 'Trừ kho lịch sử cho order seed đã thanh toán'),
  (5, 1,  2, 3, 5, 7, 'SALE_OUT', -0.40, 29.60,  80000.00, 32000.00, '2026-04-16 18:10:00', 'Trừ kho lịch sử cho order seed đã thanh toán');

-- 16.7 Nhật ký thao tác kho dùng để kiểm thử lịch sử thay đổi
INSERT IGNORE INTO inventory_action_logs (
  id, inventory_id, branch_id, branch_name, ingredient_id, ingredient_name, unit_symbol,
  acted_by, acted_by_username, acted_by_full_name, action_type,
  before_quantity, after_quantity,
  before_min_stock_level, after_min_stock_level,
  before_reorder_level, after_reorder_level,
  before_average_unit_cost, after_average_unit_cost,
  before_ingredient_name, after_ingredient_name,
  before_unit_symbol, after_unit_symbol,
  summary, occurred_at
)
VALUES
  (1, 1, 1, 'Chi nhánh Quận 1', 1,  'Thịt bò',            'kg',
   2, 'manager_q1', 'Nguyễn Văn Minh', 'CREATED',
   NULL, 50.00,
   NULL, 5.00,
   NULL, 10.00,
   NULL, 280000.00,
   NULL, 'Thịt bò',
   NULL, 'kg',
   'Tạo mới bản ghi tồn kho seed cho thịt bò tại chi nhánh Quận 1', '2026-04-10 08:00:00'),
  (2, 13, 1, 'Chi nhánh Quận 1', 13, 'Hải sản hỗn hợp',   'kg',
   2, 'manager_q1', 'Nguyễn Văn Minh', 'UPDATED',
   25.00, 0.30,
   2.00, 1.00,
   4.00, 2.00,
   150000.00, 150000.00,
   'Hải sản hỗn hợp', 'Hải sản hỗn hợp',
   'kg', 'kg',
   'Điều chỉnh tồn kho seed để tạo kịch bản WAITING_STOCK ổn định cho kiểm thử', '2026-04-17 09:00:00'),
  (3, 11, 1, 'Chi nhánh Quận 1', 11, 'Phở bò - bánh phở', 'kg',
   4, 'staff_q1_a', 'Lê Văn Hùng', 'UPDATED',
   40.00, 39.80,
   5.00, 5.00,
   10.00, 10.00,
   18000.00, 18000.00,
   'Phở bò - bánh phở', 'Phở bò - bánh phở',
   'kg', 'kg',
   'Món seed #1 tiêu thụ 0.2kg bánh phở khi bắt đầu chế biến', '2026-04-17 10:05:00');

-- 16.8 Xóa mọi liên kết gộp bàn cũ để lần seed nào cũng bắt đầu sạch
UPDATE tables
SET merged_into_table_id = NULL
WHERE id BETWEEN 1 AND 12;

-- 16.9 Trạng thái bàn đồng bộ với các kịch bản vận hành seed sẵn
UPDATE tables
SET status = CASE id
    WHEN 1 THEN 'AVAILABLE'
    WHEN 2 THEN 'OCCUPIED'
    WHEN 3 THEN 'OCCUPIED'
    WHEN 4 THEN 'CLEANING'
    WHEN 5 THEN 'RESERVED'
    WHEN 6 THEN 'AVAILABLE'
    WHEN 7 THEN 'OCCUPIED'
    WHEN 8 THEN 'AVAILABLE'
    WHEN 9 THEN 'AVAILABLE'
    WHEN 10 THEN 'AVAILABLE'
    WHEN 11 THEN 'OCCUPIED'
    WHEN 12 THEN 'OCCUPIED'
    ELSE status
END
WHERE id BETWEEN 1 AND 12;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- TÓM TẮT ID CHUẨN CHO POSTMAN
-- Vai trò:         ADMIN=1, MANAGER=2, STAFF=3, KITCHEN=4, CUSTOMER=5
-- Chi nhánh:       Quận1=1, Quận7=2, BìnhThạnh=3
-- Khu vực:         Q1TrongNha=1, Q1NgoaiTroi=2, Q7Tang1=3, Q7Tang2=4, BTChinh=5
-- Bàn Quận 1:      trống=1, đang_xử_lý=2, có_thể_xuất_bill=3, dọn_bàn=4, giữ_chỗ=5,
--                  gộp_nguồn=11, gộp_đích=12
-- Bàn Quận 7:      trống=6, bill_một_phần=7, bàn_lớn=8
-- Người dùng:      admin=1, manager_q1=2, manager_q7=3, staff_q1_a=4,
--                  staff_q1_b=5, staff_q7_a=6, kitchen_q1=7, kitchen_q7=8
-- Hạng khách hàng: Đồng=1, Bạc=2, Vàng=3, BạchKim=4, KimCương=5
-- Danh mục:        Chính=1, KhaiVị=2, TrángMiệng=3, ĐồUống=4, CơmCháo=5, HảiSản=6
-- Món ăn:          Quận1=1-10, Quận7=11-15, BìnhThạnh=16-17
-- Khách hàng:      cơ_bản=1, đồng=2, bạc=3, vàng=4, kim_cương=5
-- Tồn kho:         Quận1=1-17, Quận7=18-25
-- Đơn hàng:        đang_xử_lý=1, có_thể_xuất_bill=2, đã_thanh_toán=3, đã_hủy=4,
--                  bill_một_phần=5, gộp_nguồn=6, gộp_đích=7
-- Món trong đơn:   đang_xử_lý=1, chờ_hàng=2, đã_phục_vụ_1=3, đã_phục_vụ_2=4,
--                  gộp_nguồn_chính=10, gộp_đích_chính=11, gộp_nguồn_phụ=12, gộp_đích_phụ=13
-- Hóa đơn:         đã_thanh_toán=1, thanh_toán_một_phần=2
-- Báo cáo:         ngày thanh toán=2026-04-16, 2026-04-17;
--                  phương thức=CASH, CREDIT_CARD, E_WALLET, BANK_TRANSFER
-- Giao dịch điểm:  mở_điểm=1-4, bill1_earn=5, mở_điểm_kim_cương=6
-- Mật khẩu test:   staff_q1_b / GoldenHeart@2026
--                  email khôi phục = staff.q1b@goldenheart.com
--                  điện thoại khôi phục = 0901000004
-- =============================================================================
