# GoldenHeart Restaurant API Testing Runbook

## 1. Muc tieu

Runbook nay dung de test toan bo API hien tai cua `GoldenHeart-Restaurant` bang bo seed:

- `sql/05_seed_full_test_data.sql`
- `postman/GoldenHeart-Restaurant-E2E.postman_collection.json`
- `postman/GoldenHeart-Restaurant-E2E.postman_environment.json`

Bo collection da duoc cap nhat theo controller hien tai, bao gom:

- `Auth`
- `Roles`
- `Branches`
- `Employees`
- `Customers`
- `Inventory`
- `Menu Items`
- `Tables`
- `Orders`
- `Kitchen`
- `Billing`
- `End-to-End Flow`

## 2. Dieu kien truoc khi test

1. Start backend thanh cong tai `http://localhost:1010`
2. Hibernate da tao schema xong
3. Chay file seed:
   - `sql/05_seed_full_test_data.sql`
4. Import 2 file Postman:
   - `postman/GoldenHeart-Restaurant-E2E.postman_collection.json`
   - `postman/GoldenHeart-Restaurant-E2E.postman_environment.json`
5. Chon environment:
   - `GoldenHeart Restaurant E2E Local`

## 3. Seed va tai khoan mac dinh

### 3.1 Base URL

- `{{base_url}} = http://localhost:1010/api/v1`

### 3.2 Tai khoan dang nhap seed san

| Role | Username | Password | Ghi chu |
|---|---|---|---|
| ADMIN | `admin` | `Admin123` | Bootstrap tu app |
| MANAGER | `manager_q1` | `GoldenHeart@2026` | Branch 1 |
| STAFF | `staff_q1_a` | `GoldenHeart@2026` | Branch 1 |
| KITCHEN | `kitchen_q1` | `GoldenHeart@2026` | Branch 1 |
| Password test | `staff_q1_b` | `GoldenHeart@2026` | Dung cho change/reset password |

### 3.3 Contact dung cho password recovery

- Email: `staff.q1b@goldenheart.com`
- Phone: `0901000004`

Collection dang dung cac bien:

- `{{password_test_username}}`
- `{{password_test_old_password}}`
- `{{password_test_new_password}}`
- `{{password_test_email}}`
- `{{password_test_phone}}`
- `{{password_recovery_otp}}`
- `{{password_reset_token}}`

### 3.4 ID seed quan trong

| Nhom | Gia tri |
|---|---|
| Restaurant | `restaurant_main_id = 1` |
| Branches | `branch_q1_id = 1`, `branch_q7_id = 2`, `branch_bt_id = 3` |
| Areas | `1..5` |
| Seed customer | `seed_customer_id = 1` |
| Seed employee | `seed_employee_id = 2` |
| Seed inventory | `seed_inventory_id = 1` |
| Low stock inventory | `seed_inventory_low_stock_id = 13` |
| Seed menu item | `seed_menu_item_id = 1` |
| Out-of-stock menu item | `seed_out_of_stock_menu_item_id = 10` |
| Tables | available=`1`, processing=`2`, billable=`3`, cleaning=`4`, reserved=`5`, partial=`7` |
| Orders | processing=`1`, billable=`2`, paid=`3`, cancelled=`4`, partial=`5` |
| Order items | processing=`1`, waiting_stock=`2`, served_1=`3`, served_2=`4` |
| Bills | paid=`1`, partial=`2` |

## 4. Luu y quan trong truoc khi chay

### 4.1 Cookie refresh token

- `POST /auth/login` va `POST /auth/refresh` su dung refresh token qua cookie.
- Khi test trong Postman, cookie duoc luu tu dong neu ban dung cung environment/workspace.

### 4.2 Password APIs la request mutating

- `Change Password`
- `Request Password Recovery OTP`
- `Verify Password Recovery OTP`
- `Reset Password`

Nhung request nay thay doi password cua `staff_q1_b`.

Collection da co script doi qua lai 2 bien:

- `password_test_old_password`
- `password_test_new_password`

de giam cong suc test lap lai. Neu muon quay ve trang thai ban dau nhanh nhat, hay reseed DB.

### 4.3 OTP hien tai

- API quyen mat khau da dung OTP qua email/SMS
- Neu chua cau hinh SMTP/SMS that, app hien dang cho phep log OTP de test local
- Copy ma OTP do vao environment variable:
  - `{{password_recovery_otp}}`

### 4.4 Request co phu thuoc thu tu

Mot so request can chay dung thu tu:

- `Orders -> Create Order`
- `Kitchen -> Update Created Order Item to PROCESSING`
- `Kitchen -> Complete Created Order Item`
- `Orders -> Serve Created Order Item`
- `Billing -> Create Bill from Created Order`
- `Billing -> Add Payment to Created Bill`

Folder `End-to-End Flow` da gom san luong nay theo thu tu khuyen nghi.

## 5. Thu tu test khuyen nghi

1. `Auth -> Login Admin`
2. `Auth -> Login Manager`
3. `Auth -> Login Staff`
4. `Auth -> Login Kitchen`
5. Test cac folder read-only:
   - `Roles`
   - `Branches`
   - `Inventory`
   - `Menu Items`
   - `Tables`
6. Test CRUD:
   - `Branches`
   - `Employees`
   - `Customers`
   - `Inventory`
   - `Menu Items`
   - `Tables`
7. Test operational flow:
   - `Orders`
   - `Kitchen`
   - `Billing`
8. Test `Tables -> Split Seed Table` va `Tables -> Merge Seed Tables`
9. Test password APIs cuoi cung
10. Neu can demo nhanh mot luong hoan chinh, dung folder `End-to-End Flow` tren DB vua reseed

## 6. API chi tiet theo module

## Auth

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Register Customer | `POST /auth/register` | No auth | Tao user CUSTOMER moi | Khong lien quan customer CRM |
| Login Admin | `POST /auth/login` | No auth | `admin / Admin123` | Save `admin_token` |
| Login Manager | `POST /auth/login` | No auth | `manager_q1 / GoldenHeart@2026` | Save `manager_token` |
| Login Staff | `POST /auth/login` | No auth | `staff_q1_a / GoldenHeart@2026` | Save `staff_token` |
| Login Kitchen | `POST /auth/login` | No auth | `kitchen_q1 / GoldenHeart@2026` | Save `kitchen_token` |
| Login Password Test Staff | `POST /auth/login` | No auth | `staff_q1_b` | Save `password_test_token` |
| Refresh Token | `POST /auth/refresh` | Cookie | Refresh cookie hien tai | Save lai `auth_token` |
| Logout | `POST /auth/logout` | Cookie | Session hien tai | Clear refresh cookie |
| Change Password (Password Test Staff) | `POST /auth/change-password` | Bearer `password_test_token` | Dung `password_test_old_password` va `password_test_new_password` | Chi role MANAGER/STAFF/KITCHEN |
| Request Password Recovery OTP by Email | `POST /auth/password-recovery/request-otp` | No auth | `password_test_email` | OTP flow qua email |
| Request Password Recovery OTP by Phone | `POST /auth/password-recovery/request-otp` | No auth | `password_test_phone` | OTP flow qua SMS |
| Verify Password Recovery OTP | `POST /auth/password-recovery/verify-otp` | No auth | `password_recovery_otp` | Save `password_reset_token` |
| Reset Password | `POST /auth/password-recovery/reset-password` | No auth | `password_reset_token` | Doi mat khau user test |

## Roles

| Request name | API | Auth | Ghi chu |
|---|---|---|---|
| Get Roles | `GET /roles` | Bearer `manager_token` | Lay danh sach role he thong |

## Branches

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Get Branches | `GET /branches?restaurantId=1` | Bearer `staff_token` | `restaurant_main_id` | Role non-admin chi xem |
| Get Branch By ID | `GET /branches/{branchId}` | Bearer `staff_token` | `branch_q1_id` | Xem chi tiet chi nhanh |
| Create Branch | `POST /branches` | Bearer `admin_token` | Tao va save `created_branch_id` | Admin only |
| Update Created Branch | `PUT /branches/{created_branch_id}` | Bearer `admin_token` | `created_branch_id` | Admin only |
| Delete Created Branch | `DELETE /branches/{created_branch_id}` | Bearer `admin_token` | `created_branch_id` | Chi xoa branch vua tao |

## Employees

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Get Employees | `GET /employees?page=0&size=10` | Bearer `manager_token` | - | Manager/Admin xem list |
| Get Employee By ID | `GET /employees/{employeeId}` | Bearer `manager_token` | `seed_employee_id` | Lay chi tiet |
| Create Employee | `POST /employees` | Bearer `manager_token` | Save `created_employee_id` | Tao user + profile |
| Update Created Employee | `PUT /employees/{created_employee_id}` | Bearer `manager_token` | `created_employee_id` | Update employee vua tao |
| Delete Created Employee | `DELETE /employees/{created_employee_id}` | Bearer `admin_token` | `created_employee_id` | Delete admin only |
| Get My Profile | `GET /employees/me` | Bearer `staff_token` | - | Dung de xem `branchId`, `branchName` |
| Update My Profile | `PUT /employees/me` | Bearer `staff_token` | - | Chi sua profile cua chinh minh |

## Customers

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Get Customers | `GET /customers?page=0&size=10` | Bearer `manager_token` | - | CRUD CRM customer |
| Get Customer By ID | `GET /customers/{customerId}` | Bearer `manager_token` | `seed_customer_id` | Lay chi tiet |
| Create Customer | `POST /customers` | Bearer `manager_token` | Save `created_customer_id` | Tao customer CRM |
| Update Created Customer | `PUT /customers/{created_customer_id}` | Bearer `manager_token` | `created_customer_id` | Update customer vua tao |
| Delete Created Customer | `DELETE /customers/{created_customer_id}` | Bearer `admin_token` | `created_customer_id` | Admin only |

## Inventory

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Get Measurement Units | `GET /inventory/units` | Bearer `staff_token` | - | Danh sach don vi do luong |
| Get Inventory Items | `GET /inventory?branchId=1&page=0&size=10` | Bearer `staff_token` | `branch_q1_id` | List inventory |
| Get Inventory Summary | `GET /inventory/summary?branchId=1` | Bearer `kitchen_token` | `branch_q1_id` | Snapshot inventory value |
| Get Inventory Movement Report | `GET /inventory/reports/movements?...` | Bearer `manager_token` | Branch 1 / date range seed | Bao cao stock movement |
| Get Inventory Alerts | `GET /inventory/alerts?branchId=1` | Bearer `staff_token` | `branch_q1_id` | Low-stock / out-of-stock |
| Get Inventory By ID | `GET /inventory/{inventoryId}` | Bearer `staff_token` | `seed_inventory_id` | Detail 1 inventory item |
| Get Inventory History | `GET /inventory/{inventoryId}/history` | Bearer `manager_token` | `seed_inventory_id` | Audit log / history |
| Create Inventory Item | `POST /inventory` | Bearer `manager_token` | Save `created_inventory_id` | Manager/Admin tao moi |
| Update Created Inventory Item | `PUT /inventory/{created_inventory_id}` | Bearer `manager_token` | `created_inventory_id` | Update inventory vua tao |
| Delete Created Inventory Item | `DELETE /inventory/{created_inventory_id}` | Bearer `manager_token` | `created_inventory_id` | Xoa inventory vua tao |

## Menu Items

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Get Menu Items | `GET /menu-items?branchId=1&page=0&size=10` | Bearer `staff_token` | `branch_q1_id` | Staff/Kitchen co the xem |
| Get Menu Item By ID | `GET /menu-items/{menuItemId}` | Bearer `staff_token` | `seed_menu_item_id` | Detail menu item |
| Create Menu Item | `POST /menu-items` | Bearer `admin_token` | Save `created_menu_item_id` | Admin only |
| Update Created Menu Item | `PUT /menu-items/{created_menu_item_id}` | Bearer `admin_token` | `created_menu_item_id` | Admin only |
| Delete Created Menu Item | `DELETE /menu-items/{created_menu_item_id}` | Bearer `admin_token` | `created_menu_item_id` | Admin only |

## Tables

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Get Tables | `GET /tables?branchId=1` | Bearer `staff_token` | `branch_q1_id` | Ho tro search/filter tren API |
| Get Table By ID | `GET /tables/{tableId}` | Bearer `staff_token` | `seed_available_table_id` | Xem chi tiet ban |
| Create Table | `POST /tables` | Bearer `manager_token` | Save `created_table_id` | Admin/Manager |
| Update Created Table | `PUT /tables/{created_table_id}` | Bearer `manager_token` | `created_table_id` | Admin/Manager |
| Update Created Table Status to RESERVED | `PUT /tables/{created_table_id}/status` | Bearer `staff_token` | `created_table_id` | Staff duoc doi status |
| Get Active Order By Seed Processing Table | `GET /tables/{tableId}/active-order` | Bearer `staff_token` | `seed_processing_table_id` | Ban 2 dang co order seed |
| Split Seed Table | `POST /tables/{tableId}/split` | Bearer `staff_token` | Table 2 -> table 5 | Flow tach ban theo item |
| Merge Seed Tables | `POST /tables/merge` | Bearer `staff_token` | Table 5 -> table 2 | Flow gop ban |
| Delete Created Table | `DELETE /tables/{created_table_id}` | Bearer `admin_token` | `created_table_id` | Chi xoa ban vua tao |

## Orders

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Create Order | `POST /orders` | Bearer `staff_token` | Table 1, customer 1 | Save `created_order_id`, `created_order_item_1_id` |
| Get Created Order By ID | `GET /orders/{created_order_id}` | Bearer `staff_token` | `created_order_id` | Sau khi tao order |
| Serve Created Order Item | `PUT /orders/order-items/{created_order_item_1_id}/serve` | Bearer `staff_token` | `created_order_item_1_id` | Chay sau kitchen complete |

## Kitchen

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Get Pending Kitchen Items | `GET /kitchen/orders/pending?branchId=1` | Bearer `kitchen_token` | `branch_q1_id` | List item cho bep |
| Update Created Order Item to PROCESSING | `PUT /kitchen/order-items/{id}/status` | Bearer `kitchen_token` | `created_order_item_1_id` | Triggers stock deduction khi du ingredient |
| Complete Created Order Item | `POST /kitchen/order-items/{id}/complete` | Bearer `kitchen_token` | `created_order_item_1_id` | Chuyen PROCESSING -> COMPLETED |

## Billing

| Request name | API | Auth | Seed / bien dung | Ghi chu |
|---|---|---|---|---|
| Create Bill from Created Order | `POST /bills` | Bearer `staff_token` | `created_order_id` | Chay sau khi item da SERVED |
| Add Payment to Created Bill | `POST /bills/{billId}/payments` | Bearer `staff_token` | `created_bill_id`, `created_bill_remaining_amount` | Thanh toan phan con lai |

## End-to-End Flow

Folder nay dung de demo nhanh luong:

1. Staff login
2. Tao order
3. Kitchen login
4. Chuyen item sang PROCESSING
5. Complete item
6. Staff serve item
7. Tao bill
8. Thanh toan

Request trong folder nay dung cac bien:

- `e2e_order_id`
- `e2e_order_item_1_id`
- `e2e_bill_id`
- `e2e_bill_remaining_amount`

Nen chay folder nay tren DB vua reseed de tranh xung dot voi state da bi mutate tu cac folder khac.

## 7. Mau luong test thanh toan dung nghiep vu

De API thanh toan pass dung logic hien tai:

1. `Auth -> Login Staff`
2. `Auth -> Login Kitchen`
3. `Orders -> Create Order`
4. `Kitchen -> Update Created Order Item to PROCESSING`
5. `Kitchen -> Complete Created Order Item`
6. `Orders -> Serve Created Order Item`
7. `Billing -> Create Bill from Created Order`
8. `Billing -> Add Payment to Created Bill`

Neu bo qua buoc `Serve`, request tao bill se fail do order item chua hop le de thanh toan.

## 8. Mau luong test password

### Change password

1. `Auth -> Login Password Test Staff`
2. `Auth -> Change Password (Password Test Staff)`
3. `Auth -> Login Password Test Staff`

Collection da tu dong swap:

- `password_test_old_password`
- `password_test_new_password`

### Forgot password by OTP

1. `Auth -> Request Password Recovery OTP by Email`
2. Copy OTP tu email/log vao `{{password_recovery_otp}}`
3. `Auth -> Verify Password Recovery OTP`
4. `Auth -> Reset Password`
5. `Auth -> Login Password Test Staff`

## 9. Khi nao can reseed DB

Nen reseed lai neu:

- Ban da chay `End-to-End Flow` nhieu lan
- Ban da split/merge table seed
- Ban da doi/reset password va muon quay lai trang thai goc
- Ban muon demo lai dung state seed ban dau

Neu chi test cac API GET hoac CRUD tren resource moi tao roi xoa ngay, khong bat buoc reseed.
