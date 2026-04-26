# GoldenHeart Restaurant API Testing Runbook

## 1. Mục tiêu

Tài liệu này dùng để test end-to-end toàn bộ API hiện tại của dự án `GoldenHeart-Restaurant` bằng bộ artifact đã được đồng bộ sẵn:

- `sql/05_seed_full_test_data.sql`
- `postman/GoldenHeart-Restaurant-E2E.postman_collection.json`
- `postman/GoldenHeart-Restaurant-E2E.postman_environment.json`

Bộ test hiện tại đã bao gồm các nhóm chức năng chính:

- Xác thực và quản lý mật khẩu
- Danh mục món ăn
- Chi nhánh
- Khu vực bàn
- Bàn
- Nhân viên
- Khách hàng và loyalty
- Kho
- Món ăn
- Đơn hàng
- Bếp
- Hóa đơn và thanh toán
- Báo cáo dashboard và doanh thu

Lưu ý:

- Repo hiện tại không còn dùng `schema.sql` làm nguồn chính cho local test.
- Dữ liệu test chuẩn được lấy từ `05_seed_full_test_data.sql`.
- Loyalty hiện dùng dữ liệu tier trong database, không còn bootstrap tự động khi khởi động ứng dụng.

## 2. Điều kiện trước khi test

1. Khởi động backend thành công tại `http://localhost:1010`
2. Hibernate đã tạo xong schema
3. Chạy file seed:
   - `sql/05_seed_full_test_data.sql`
4. Import 2 file Postman:
   - `postman/GoldenHeart-Restaurant-E2E.postman_collection.json`
   - `postman/GoldenHeart-Restaurant-E2E.postman_environment.json`
5. Chọn environment:
   - `GoldenHeart Restaurant E2E Local`

## 3. Tài khoản test có sẵn

| Vai trò | Username | Password | Ghi chú |
|---|---|---|---|
| ADMIN | `admin` | `Admin123` | Tài khoản bootstrap của hệ thống |
| MANAGER | `manager_q1` | `GoldenHeart@2026` | Làm việc tại chi nhánh Quận 1 |
| STAFF | `staff_q1_a` | `GoldenHeart@2026` | Làm việc tại chi nhánh Quận 1 |
| KITCHEN | `kitchen_q1` | `GoldenHeart@2026` | Làm việc tại chi nhánh Quận 1 |
| Test mật khẩu | `staff_q1_b` | `GoldenHeart@2026` | Dùng cho luồng đổi/quên mật khẩu |

## 4. Dữ liệu seed quan trọng

### 4.1 Base URL

- `{{base_url}} = http://localhost:1010/api/v1`

### 4.2 Customer tiers

Tier loyalty hiện được coi là dữ liệu cấu hình nghiệp vụ và lấy trực tiếp từ database.

| Tier | ID | Điểm tối thiểu | Giảm giá |
|---|---:|---:|---:|
| Bronze | `1` | `100` | `1%` |
| Silver | `2` | `200` | `2%` |
| Gold | `3` | `500` | `3%` |
| Platinum | `4` | `800` | `4%` |
| Diamond | `5` | `1000` | `5%` |

### 4.3 Khách hàng seed để test loyalty

| Khách hàng | ID | Điểm hiện có | Hạng hiện tại |
|---|---:|---:|---|
| Khách cơ bản | `1` | `80` | Chưa lên hạng |
| Khách Bronze | `2` | `150` | Bronze |
| Khách Silver | `3` | `210` | Silver |
| Khách Gold | `4` | `542` | Gold |
| Khách Diamond | `5` | `1005` | Diamond |

### 4.4 Khu vực bàn seed

| Khu vực | ID | Chi nhánh |
|---|---:|---|
| Khu trong nhà Quận 1 | `1` | Branch 1 |
| Khu ngoài trời Quận 1 | `2` | Branch 1 |
| Tầng 1 Quận 7 | `3` | Branch 2 |
| Tầng 2 Quận 7 | `4` | Branch 2 |
| Khu chính Bình Thạnh | `5` | Branch 3 |

### 4.5 Đơn hàng và hóa đơn seed liên quan loyalty

| Đối tượng | ID | Ghi chú |
|---|---:|---|
| Order đang xử lý | `1` | Có món `PROCESSING` và `WAITING_STOCK` |
| Order có thể xuất bill | `2` | Dùng để preview discount loyalty |
| Order đã thanh toán | `3` | Đã sinh bill `1` và đã cộng điểm |
| Order đã hủy | `4` | Dùng để test trạng thái `CANCELLED` |
| Order bill một phần | `5` | Bill `2` đang ở trạng thái `PARTIAL` |
| Bill đã thanh toán | `1` | Bill seed đã hoàn tất |
| Bill thanh toán một phần | `2` | Bill seed đang `PARTIAL` |

### 4.6 Dữ liệu seed cho reports

Seed hiện có dữ liệu để test báo cáo theo các mốc sau:

- Ngày có thanh toán: `2026-04-16`, `2026-04-17`
- Phương thức thanh toán có đủ:
  - `CASH`
  - `CREDIT_CARD`
  - `E_WALLET`
  - `BANK_TRANSFER`

Biến gợi ý để test report:

- `report_anchor_date = 2026-04-17`
- `report_from_date = 2026-04-15`
- `report_to_date = 2026-04-17`

### 4.7 Dữ liệu recovery password

- Email recovery: `staff.q1b@goldenheart.com`
- Số điện thoại recovery: `0901000004`

## 5. Biến Postman quan trọng

### 5.1 Tokens

- `admin_token`
- `manager_token`
- `staff_token`
- `kitchen_token`
- `password_test_token`

### 5.2 Seed IDs chính

- `restaurant_main_id = 1`
- `role_admin_id = 1`
- `role_manager_id = 2`
- `role_staff_id = 3`
- `role_kitchen_id = 4`
- `role_customer_id = 5`
- `branch_q1_id = 1`
- `branch_q7_id = 2`
- `branch_bt_id = 3`
- `area_q1_indoor_id = 1`
- `area_q1_outdoor_id = 2`
- `area_q7_floor1_id = 3`
- `area_q7_floor2_id = 4`
- `area_bt_main_id = 5`
- `seed_category_id = 1`
- `seed_customer_id = 1`
- `seed_customer_bronze_id = 2`
- `seed_customer_silver_id = 3`
- `seed_customer_gold_id = 4`
- `seed_customer_diamond_id = 5`
- `seed_customer_tier_bronze_id = 1`
- `seed_customer_tier_silver_id = 2`
- `seed_customer_tier_gold_id = 3`
- `seed_customer_tier_platinum_id = 4`
- `seed_customer_tier_diamond_id = 5`
- `seed_available_table_id = 1`
- `seed_processing_table_id = 2`
- `seed_billable_table_id = 3`
- `seed_cleaning_table_id = 4`
- `seed_reserved_table_id = 5`
- `seed_partial_bill_table_id = 7`
- `seed_order_processing_id = 1`
- `seed_order_billable_id = 2`
- `seed_order_paid_id = 3`
- `seed_order_cancelled_id = 4`
- `seed_order_partial_bill_id = 5`
- `seed_order_item_processing_id = 1`
- `seed_order_item_waiting_stock_id = 2`
- `seed_order_item_served_1_id = 3`
- `seed_order_item_served_2_id = 4`
- `seed_bill_paid_id = 1`
- `seed_bill_partial_id = 2`

### 5.3 Runtime IDs được lưu tự động bởi script Postman

- `created_branch_id`
- `created_category_id`
- `created_employee_id`
- `created_customer_id`
- `created_quick_customer_id`
- `created_customer_tier_id`
- `created_inventory_id`
- `created_menu_item_id`
- `created_dining_area_id`
- `created_table_id`
- `created_order_id`
- `created_order_item_1_id`
- `created_order_item_2_id`
- `created_bill_id`
- `created_bill_remaining_amount`
- `e2e_order_id`
- `e2e_order_item_1_id`
- `e2e_order_item_2_id`
- `e2e_bill_id`
- `e2e_bill_remaining_amount`

### 5.4 Biến cho password flow

- `password_test_username`
- `password_test_old_password`
- `password_test_new_password`
- `password_test_email`
- `password_test_phone`
- `password_recovery_otp`
- `password_reset_token`

### 5.5 Biến cho report flow

- `report_anchor_date = 2026-04-17`
- `report_from_date = 2026-04-15`
- `report_to_date = 2026-04-17`

## 6. Lưu ý quan trọng trước khi chạy

### 6.1 Password APIs có thay đổi trạng thái tài khoản test

Các request sau sẽ thay đổi mật khẩu của `staff_q1_b`:

- `Change Password (Password Test Staff)`
- `Request Password Recovery OTP by Email`
- `Request Password Recovery OTP by Phone`
- `Verify Password Recovery OTP`
- `Reset Password`

Nếu muốn quay về trạng thái ban đầu nhanh nhất, hãy reseed lại database.

### 6.2 OTP local

- Nếu chưa cấu hình SMTP hoặc SMS thật, OTP có thể đang chạy theo mode local/dev.
- Sau khi lấy được OTP, điền vào biến:
  - `{{password_recovery_otp}}`

### 6.3 Loyalty points chỉ cộng khi bill đã PAID

- Preview chỉ tính toán trước, không cộng điểm.
- Bill `PARTIAL` chưa cộng điểm.
- Bill chỉ cộng điểm một lần khi chuyển sang `PAID`.

### 6.4 Loyalty discount dựa trên điểm hiện có trước thanh toán

- Điểm phát sinh từ bill hiện tại chỉ dùng cho lần thanh toán sau.
- Khách mới tạo nhanh ở quầy có thể chưa được giảm ở bill đầu tiên.

### 6.5 Reports đang dùng dữ liệu seed cố định

- Nếu bạn tạo thêm bill hoặc payment trong lúc test, số liệu reports sẽ thay đổi.
- Muốn đối chiếu đúng như runbook, hãy reseed database trước khi test module `Reports`.

## 7. Thứ tự test khuyến nghị

1. Đăng nhập các role:
   - `Auth -> Login Admin`
   - `Auth -> Login Manager`
   - `Auth -> Login Staff`
   - `Auth -> Login Kitchen`
2. Test read-only nhanh:
   - `Roles`
   - `Categories`
   - `Branches`
   - `Dining Areas`
   - `Inventory`
   - `Menu Items`
   - `Tables`
   - `Customer Tiers`
   - `Reports`
3. Test CRUD:
   - `Categories`
   - `Branches`
   - `Dining Areas`
   - `Employees`
   - `Customers`
   - `Customer Tiers`
   - `Inventory`
   - `Menu Items`
   - `Tables`
4. Test order, kitchen, billing:
   - `Orders`
   - `Kitchen`
   - `Billing`
5. Test loyalty flow:
   - `Customers -> Lookup Customers`
   - `Customers -> Quick Create Customer`
   - `Orders -> Assign Quick Customer To Created Order`
   - `Billing -> Preview Seed Billable Order with Loyalty`
   - `Billing -> Preview Created Order with Loyalty`
   - `Billing -> Create Bill from Created Order`
   - `Billing -> Add Payment to Created Bill`
   - `Customers -> Get Loyalty Transactions for Seed Gold Customer`
6. Test dashboard và reports:
   - `Reports -> Get Dashboard Report`
   - `Reports -> Get Revenue Summary - Day`
   - `Reports -> Get Revenue Summary - Month`
   - `Reports -> Get Revenue Timeseries`
   - `Reports -> Get Payment Method Breakdown`
   - `Reports -> Get Bill Status Summary`
7. Test full luồng demo:
   - chạy folder `End-to-End Flow`
8. Test password APIs cuối cùng

## 8. Danh sách folder Postman

- `Auth`
- `Roles`
- `Categories`
- `Branches`
- `Employees`
- `Customers`
- `Customer Tiers`
- `Inventory`
- `Menu Items`
- `Dining Areas`
- `Tables`
- `Orders`
- `Kitchen`
- `Billing`
- `Reports`
- `End-to-End Flow`

## 9. API chi tiết theo module

## Auth

| Request name | API | Auth | Mục đích / phụ thuộc |
|---|---|---|---|
| Register Customer | `POST /auth/register` | No auth | Luồng legacy, không phải luồng CRM loyalty hiện tại |
| Login Admin | `POST /auth/login` | No auth | Lưu `admin_token` |
| Login Manager | `POST /auth/login` | No auth | Lưu `manager_token` |
| Login Staff | `POST /auth/login` | No auth | Lưu `staff_token` |
| Login Kitchen | `POST /auth/login` | No auth | Lưu `kitchen_token` |
| Login Password Test Staff | `POST /auth/login` | No auth | Lưu `password_test_token` |
| Refresh Token | `POST /auth/refresh` | Cookie | Làm mới phiên đăng nhập |
| Logout | `POST /auth/logout` | Cookie | Xóa refresh session |
| Change Password (Password Test Staff) | `POST /auth/change-password` | Bearer `password_test_token` | Đổi mật khẩu khi đã đăng nhập |
| Request Password Recovery OTP by Email | `POST /auth/password-recovery/request-otp` | No auth | Bắt đầu quên mật khẩu bằng email |
| Request Password Recovery OTP by Phone | `POST /auth/password-recovery/request-otp` | No auth | Bắt đầu quên mật khẩu bằng số điện thoại |
| Verify Password Recovery OTP | `POST /auth/password-recovery/verify-otp` | No auth | Xác minh OTP và lưu `password_reset_token` |
| Reset Password | `POST /auth/password-recovery/reset-password` | No auth | Đặt lại mật khẩu bằng OTP |

## Roles

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Roles | `GET /roles` | Bearer `manager_token` | Lấy danh sách role hệ thống |

## Categories

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Categories | `GET /categories?page=0&size=10` | Bearer `staff_token` | Lấy danh sách danh mục món ăn |
| Get Category By ID | `GET /categories/{categoryId}` | Bearer `staff_token` | Xem chi tiết danh mục seed |
| Create Category | `POST /categories` | Bearer `admin_token` | Tạo danh mục mới, lưu `created_category_id` |
| Update Created Category | `PUT /categories/{created_category_id}` | Bearer `admin_token` | Sửa danh mục vừa tạo |
| Delete Created Category | `DELETE /categories/{created_category_id}` | Bearer `admin_token` | Xóa danh mục vừa tạo |

## Branches

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Branches | `GET /branches?restaurantId=1` | Bearer `staff_token` | Xem danh sách chi nhánh |
| Get Branch By ID | `GET /branches/{branchId}` | Bearer `staff_token` | Xem chi tiết chi nhánh |
| Create Branch | `POST /branches` | Bearer `admin_token` | Tạo chi nhánh mới, lưu `created_branch_id` |
| Update Created Branch | `PUT /branches/{created_branch_id}` | Bearer `admin_token` | Sửa chi nhánh vừa tạo |
| Delete Created Branch | `DELETE /branches/{created_branch_id}` | Bearer `admin_token` | Xóa chi nhánh vừa tạo |

## Employees

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Employees | `GET /employees?page=0&size=10` | Bearer `manager_token` | Lấy danh sách nhân viên |
| Get Employee By ID | `GET /employees/{employeeId}` | Bearer `manager_token` | Xem chi tiết nhân viên seed |
| Create Employee | `POST /employees` | Bearer `manager_token` | Tạo nhân viên mới, lưu `created_employee_id` |
| Update Created Employee | `PUT /employees/{created_employee_id}` | Bearer `manager_token` | Sửa nhân viên vừa tạo |
| Delete Created Employee | `DELETE /employees/{created_employee_id}` | Bearer `admin_token` | Xóa nhân viên vừa tạo |
| Get My Profile | `GET /employees/me` | Bearer `staff_token` | Xem hồ sơ và chi nhánh đang làm việc |
| Update My Profile | `PUT /employees/me` | Bearer `staff_token` | Sửa hồ sơ của chính mình |

## Customers

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Customers | `GET /customers?page=0&size=10` | Bearer `manager_token` | CRUD khách hàng CRM |
| Lookup Customers | `GET /customers/lookup?keyword={{seed_customer_lookup_keyword}}&size=5` | Bearer `staff_token` | Tìm nhanh khách hàng trong luồng POS |
| Get Customer By ID | `GET /customers/{customerId}` | Bearer `manager_token` | Xem chi tiết khách hàng seed |
| Get Loyalty Transactions for Seed Gold Customer | `GET /customers/{customerId}/loyalty-transactions?page=0&size=10` | Bearer `staff_token` | Xem lịch sử điểm của khách Gold |
| Create Customer | `POST /customers` | Bearer `manager_token` | Tạo khách hàng CRM, lưu `created_customer_id` |
| Quick Create Customer | `POST /customers/quick-create` | Bearer `staff_token` | Tạo nhanh khách vãng lai, lưu `created_quick_customer_id` |
| Update Created Customer | `PUT /customers/{created_customer_id}` | Bearer `manager_token` | Sửa khách hàng vừa tạo |
| Delete Created Customer | `DELETE /customers/{created_customer_id}` | Bearer `admin_token` | Xóa khách hàng vừa tạo |

## Customer Tiers

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Customer Tiers | `GET /customer-tiers?activeOnly=true` | Bearer `staff_token` | Lấy danh sách tier đang active |
| Get Customer Tier By ID | `GET /customer-tiers/{tierId}` | Bearer `staff_token` | Xem chi tiết tier seed |
| Create Customer Tier | `POST /customer-tiers` | Bearer `admin_token` | Tạo tier mới, lưu `created_customer_tier_id` |
| Update Created Customer Tier | `PUT /customer-tiers/{created_customer_tier_id}` | Bearer `admin_token` | Sửa tier vừa tạo |
| Deactivate Created Customer Tier | `DELETE /customer-tiers/{created_customer_tier_id}` | Bearer `admin_token` | Ngừng kích hoạt tier vừa tạo |

## Inventory

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Measurement Units | `GET /inventory/units` | Bearer `staff_token` | Lấy danh sách đơn vị tính |
| Get Inventory Items | `GET /inventory?branchId=1&page=0&size=10` | Bearer `staff_token` | Lấy danh sách kho |
| Get Inventory Summary | `GET /inventory/summary?branchId=1` | Bearer `kitchen_token` | Xem snapshot giá trị tồn kho hiện tại |
| Get Inventory Movement Report | `GET /inventory/reports/movements?...` | Bearer `manager_token` | Báo cáo biến động kho theo ngày/tháng |
| Get Inventory Alerts | `GET /inventory/alerts?branchId=1` | Bearer `staff_token` | Xem cảnh báo low-stock và out-of-stock |
| Get Inventory By ID | `GET /inventory/{inventoryId}` | Bearer `staff_token` | Xem chi tiết item kho |
| Get Inventory History | `GET /inventory/{inventoryId}/history` | Bearer `manager_token` | Xem lịch sử audit của item kho |
| Create Inventory Item | `POST /inventory` | Bearer `manager_token` | Tạo item kho mới, lưu `created_inventory_id` |
| Update Created Inventory Item | `PUT /inventory/{created_inventory_id}` | Bearer `manager_token` | Sửa item kho vừa tạo |
| Delete Created Inventory Item | `DELETE /inventory/{created_inventory_id}` | Bearer `manager_token` | Xóa item kho vừa tạo |

## Menu Items

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Menu Items | `GET /menu-items?branchId=1&page=0&size=10` | Bearer `staff_token` | Lấy danh sách món ăn |
| Get Menu Item By ID | `GET /menu-items/{menuItemId}` | Bearer `staff_token` | Xem chi tiết món seed |
| Create Menu Item | `POST /menu-items` | Bearer `admin_token` | Tạo món mới, lưu `created_menu_item_id` |
| Update Created Menu Item | `PUT /menu-items/{created_menu_item_id}` | Bearer `admin_token` | Sửa món vừa tạo |
| Delete Created Menu Item | `DELETE /menu-items/{created_menu_item_id}` | Bearer `admin_token` | Xóa món vừa tạo |

## Dining Areas

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Dining Areas | `GET /dining-areas?branchId=1&active=true&q=Khu` | Bearer `staff_token` | Lấy danh sách khu vực bàn theo chi nhánh |
| Get Dining Area By ID | `GET /dining-areas/{areaId}` | Bearer `staff_token` | Xem chi tiết khu vực bàn seed |
| Create Dining Area | `POST /dining-areas` | Bearer `manager_token` | Tạo khu vực mới, lưu `created_dining_area_id` |
| Update Created Dining Area | `PUT /dining-areas/{created_dining_area_id}` | Bearer `manager_token` | Sửa khu vực vừa tạo |
| Delete Created Dining Area | `DELETE /dining-areas/{created_dining_area_id}` | Bearer `admin_token` | Xóa khu vực vừa tạo |

## Tables

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Tables | `GET /tables?branchId=1` | Bearer `staff_token` | Xem danh sách bàn |
| Get Table By ID | `GET /tables/{tableId}` | Bearer `staff_token` | Xem chi tiết bàn |
| Create Table | `POST /tables` | Bearer `manager_token` | Tạo bàn mới, lưu `created_table_id` |
| Update Created Table | `PUT /tables/{created_table_id}` | Bearer `manager_token` | Sửa bàn vừa tạo |
| Update Created Table Status to RESERVED | `PUT /tables/{created_table_id}/status` | Bearer `staff_token` | Test quyền staff đổi trạng thái bàn vận hành |
| Get Active Order By Seed Processing Table | `GET /tables/{tableId}/active-order` | Bearer `staff_token` | Xem order đang mở của bàn seed |
| Split Seed Table | `POST /tables/{tableId}/split` | Bearer `staff_token` | Test tách bàn |
| Merge Seed Tables | `POST /tables/merge` | Bearer `staff_token` | Test gộp bàn |
| Delete Created Table | `DELETE /tables/{created_table_id}` | Bearer `admin_token` | Xóa bàn vừa tạo |

## Orders

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Create Order | `POST /orders` | Bearer `staff_token` | Tạo order mới, lưu `created_order_id` và order item IDs |
| Get Created Order By ID | `GET /orders/{created_order_id}` | Bearer `staff_token` | Xem lại order vừa tạo |
| Assign Quick Customer To Created Order | `PUT /orders/{created_order_id}/customer` | Bearer `staff_token` | Gắn khách vào order để áp loyalty lúc checkout |
| Serve Created Order Item | `PUT /orders/order-items/{created_order_item_1_id}/serve` | Bearer `staff_token` | Đánh dấu món đã phục vụ |

## Kitchen

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Pending Kitchen Items | `GET /kitchen/orders/pending` | Bearer `kitchen_token` | Xem hàng chờ bếp |
| Update Created Order Item to PROCESSING | `PUT /kitchen/order-items/{created_order_item_1_id}/status` | Bearer `kitchen_token` | Chuyển món sang `PROCESSING` |
| Complete Created Order Item | `POST /kitchen/order-items/{created_order_item_1_id}/complete` | Bearer `kitchen_token` | Hoàn tất món |

## Billing

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Preview Seed Billable Order with Loyalty | `GET /bills/preview?orderId={{seed_order_billable_id}}&discount=0&taxRate=0&applyLoyaltyDiscount=true` | Bearer `staff_token` | Xem discount loyalty của order Bronze seed |
| Preview Created Order with Loyalty | `GET /bills/preview?orderId={{created_order_id}}&discount=0&taxRate=0&applyLoyaltyDiscount=true` | Bearer `staff_token` | Preview order vừa tạo sau khi đã gắn customer |
| Create Bill from Created Order | `POST /bills` | Bearer `staff_token` | Tạo bill cho created order, lưu `created_bill_id` và `created_bill_remaining_amount` |
| Add Payment to Created Bill | `POST /bills/{created_bill_id}/payments` | Bearer `staff_token` | Thanh toán phần còn lại, nếu đủ tiền bill sẽ thành `PAID` và khách được cộng điểm |

## Reports

| Request name | API | Auth | Mục đích |
|---|---|---|---|
| Get Dashboard Report | `GET /reports/dashboard?branchId=1` | Bearer `staff_token` | Xem snapshot dashboard của chi nhánh |
| Get Revenue Summary - Day | `GET /reports/revenue/summary?branchId=1&periodType=DAY&anchorDate={{report_anchor_date}}` | Bearer `manager_token` | Tổng hợp số bill, doanh thu và lợi nhuận theo ngày |
| Get Revenue Summary - Month | `GET /reports/revenue/summary?branchId=1&periodType=MONTH&anchorDate={{report_anchor_date}}` | Bearer `manager_token` | Tổng hợp theo tháng |
| Get Revenue Timeseries | `GET /reports/revenue/timeseries?branchId=1&fromDate={{report_from_date}}&toDate={{report_to_date}}&groupBy=DAY` | Bearer `manager_token` | Dữ liệu chart doanh thu theo ngày |
| Get Payment Method Breakdown | `GET /reports/payments/method-breakdown?branchId=1&periodType=MONTH&anchorDate={{report_anchor_date}}` | Bearer `manager_token` | Breakdown thanh toán theo phương thức |
| Get Bill Status Summary | `GET /reports/bills/status-summary?branchId=1` | Bearer `manager_token` | Snapshot bill `UNPAID/PARTIAL/PAID` |

## End-to-End Flow

Folder này dùng để demo toàn bộ luồng loyalty cơ bản:

1. `Login Staff (E2E)`
2. `Quick Create E2E Customer`
3. `Create E2E Order`
4. `Assign E2E Customer To Order`
5. `Login Kitchen (E2E)`
6. `Update E2E Item to PROCESSING`
7. `Complete E2E Item`
8. `Serve E2E Item`
9. `Preview E2E Bill with Loyalty`
10. `Create E2E Bill`
11. `Add Payment to E2E Bill`
12. `Get E2E Customer Loyalty Transactions`

Kỳ vọng:

- Khách mới tạo nhanh ban đầu chưa có điểm, tier có thể là `null`
- Preview bill có thể đang áp loyalty discount bằng `0`
- Sau khi thanh toán đủ, customer sẽ có transaction `EARN`
- API lịch sử loyalty sẽ trả về transaction vừa phát sinh

## 10. Lỗi thường gặp

### 10.1 401 sau khi login thành công

- Kiểm tra request đã gửi đúng `Bearer {{staff_token}}` hoặc token role tương ứng chưa
- Chạy lại request login để cập nhật token mới

### 10.2 Create Bill bị conflict

Nguyên nhân thường gặp:

- Món chưa ở trạng thái đủ điều kiện để thanh toán
- Bill đã có payment nên không được recalculated
- Order không còn billable items

### 10.3 Loyalty points không tăng

Kiểm tra:

- Bill đã thực sự `PAID` chưa
- Order đã được gắn `customer` chưa
- Đang xem đúng customer ID chưa

### 10.4 Quick Create Customer bị conflict

Nếu `phone` hoặc `email` trùng với khách hàng đã có, API sẽ từ chối để tránh tạo bản ghi trùng.

### 10.5 Report không khớp số liệu mong đợi

Kiểm tra:

- Database có vừa được reseed hay chưa
- Trong quá trình test có phát sinh thêm bill hoặc payment mới không
- Đã dùng đúng khoảng ngày trong biến `report_anchor_date`, `report_from_date`, `report_to_date` chưa
