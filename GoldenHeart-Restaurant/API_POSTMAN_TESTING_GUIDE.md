# Hướng dẫn test API bằng Postman cho GoldenHeart Restaurant

Tài liệu này dùng cho trạng thái hiện tại của dự án, bám theo đúng:

- collection Postman đang có trong thư mục `postman`
- environment local đang có trong thư mục `postman`
- bộ SQL đã được gộp còn 3 file trong thư mục `sql`

Mục tiêu của tài liệu:

- giúp bạn test được cả API cũ và API mới theo đúng thứ tự
- tránh trường hợp DB trống làm API lỗi dù code không sai
- chỉ rõ request nào cần chạy trước, request nào chỉ chạy được một lần
- giải thích biến môi trường nào sẽ được Postman tự cập nhật

---

## 1. Các file bạn sẽ dùng

### 1.1 File Postman

- `postman/GoldenHeart-Restaurant.postman_collection.json`
- `postman/GoldenHeart-Restaurant.local.postman_environment.json`

### 1.2 File SQL

- `sql/01_reset_local_database.sql`
- `sql/02_seed_reference_data.sql`
- `sql/03_postman_db_check_queries.sql`

Ý nghĩa:

- `01`: reset DB local về trạng thái sạch
- `02`: seed dữ liệu mẫu để DB không còn trống
- `03`: query kiểm tra DB sau khi test API

---

## 2. Các nhóm API hiện có trong collection

Collection hiện tại có các folder sau:

- `Auth`
- `Roles`
- `Employees`
- `Menu`
- `Kitchen`
- `Inventory`
- `Customers`

Tức là hiện tại bạn có thể test được các nhóm nghiệp vụ chính sau:

- xác thực và vòng đời token
- phân quyền và vai trò
- CRUD nhân viên
- CRUD khách hàng
- CRUD menu và recipe
- hoàn thành món từ bếp và tự động trừ kho
- quản lý inventory, lịch sử thay đổi, low-stock alerts, đơn vị tính

---

## 3. Chuẩn bị trước khi test

### 3.1 Chuẩn bị database

Thứ tự đúng:

1. Chạy `sql/01_reset_local_database.sql`
2. Start Spring Boot app 1 lần
3. Chạy `sql/02_seed_reference_data.sql`

Vì sao phải làm như vậy:

- file `01` chỉ tạo lại database rỗng
- khi app start, Hibernate mới tạo schema và bootstrap dữ liệu hệ thống như role/admin
- file `02` cần các bảng đã tồn tại để thêm dữ liệu mẫu phục vụ test Postman

### 3.2 Start backend

Chạy app như bình thường bằng:

- `./gradlew bootRun`
- hoặc `Shift + F10` trong IntelliJ

Base URL local hiện tại:

```text
http://localhost:1010
```

### 3.3 Dữ liệu hệ thống sẽ có sau khi app start

Trên DB sạch, sau khi app start thành công bạn nên có:

- các role hệ thống: `ADMIN`, `MANAGER`, `STAFF`, `KITCHEN`, `CUSTOMER`
- tài khoản admin mặc định:
  - username: `admin`
  - password: `Admin123`

---

## 4. Import Postman đúng cách

### 4.1 Import collection

Trong Postman:

1. bấm `Import`
2. chọn file `GoldenHeart-Restaurant.postman_collection.json`

### 4.2 Import environment

Trong Postman:

1. bấm `Import`
2. chọn file `GoldenHeart-Restaurant.local.postman_environment.json`
3. ở góc phải trên cùng, chọn environment `GoldenHeart Restaurant Local`

### 4.3 Lưu ý về tab `Scripts`

Nhiều request trong collection đã có script sẵn trong tab `Scripts`.

Các script này dùng để:

- lưu `accessToken`
- lưu `managerId`, `staffId`, `customerId`
- lưu `createdMenuItemId`
- lưu `createdInventoryId`
- lưu các `unitId`

Nghĩa là trong đa số trường hợp, bạn chỉ cần chạy đúng thứ tự, không cần copy tay ID quá nhiều.

### 4.4 Tab `Test Results`

Sau khi bấm `Send`, ở phần response phía dưới bạn có thể mở tab:

- `Body`
- `Headers`
- `Cookies`
- `Test Results`

Nếu request có script test, kết quả pass/fail sẽ hiện ở `Test Results`.

---

## 5. Các biến môi trường quan trọng

Environment local hiện có các biến nền sau:

- `baseUrl = http://localhost:1010`
- `adminUsername = admin`
- `adminPassword = Admin123`
- `managerUsername = manager01`
- `managerPassword = Manager123`
- `staffUsername = staff01`
- `staffPassword = Staff123`
- `customerUsername = customer01`
- `customerPassword = Customer123`

Các biến dữ liệu seed có sẵn:

- `branch1Id = 1`
- `phoCategoryId = 1`
- `miCategoryId = 2`
- `ingredientBeefId = 1`
- `ingredientRiceNoodleId = 2`
- `ingredientBrothId = 3`
- `ingredientOnionId = 4`
- `kgUnitId = 1`
- `pieceUnitId = 2`
- `literUnitId = 3`
- `gramUnitId = 4`
- `milliliterUnitId = 5`
- `menuItemId = 1`
- `orderItemId = 1`
- `inventoryId = 1`

Các biến sẽ được Postman cập nhật dần khi bạn test:

- `adminAccessToken`
- `managerAccessToken`
- `staffAccessToken`
- `customerAccessToken`
- `managerRoleId`
- `staffRoleId`
- `managerId`
- `staffId`
- `customerId`
- `createdMenuItemId`
- `createdInventoryId`

---

## 6. Dữ liệu mẫu có sẵn sau khi chạy file seed

Sau khi chạy `sql/02_seed_reference_data.sql`, bạn đã có sẵn dữ liệu đủ để test nhanh:

- nhà hàng `GoldenHeart Restaurant`
- chi nhánh:
  - `branchId = 1`
  - `branchId = 2`
- category:
  - `categoryId = 1` là `Pho`
  - `categoryId = 2` là `Mon Them`
- nguyên liệu:
  - `ingredientId = 1` là `Beef`
  - `ingredientId = 2` là `Rice Noodle`
  - `ingredientId = 3` là `Broth`
  - `ingredientId = 4` là `Onion`
- inventory seed:
  - `inventoryId = 1` trở đi
- menu seed:
  - `menuItemId = 1` là `Pho Bo Tai`
- kitchen seed:
  - `orderItemId = 1` là món pending để test complete

Nhờ vậy bạn không cần tự thêm dữ liệu nền bằng tay trước khi test menu, inventory, kitchen.

---

## 7. Thứ tự test khuyến nghị từ database trống

Đây là thứ tự mình khuyên dùng để ít lỗi nhất:

1. reset DB
2. start backend
3. seed dữ liệu mẫu
4. login admin
5. lấy roles
6. tạo manager
7. login manager
8. tạo staff
9. login staff
10. test profile của staff
11. test CRUD customer
12. test GET menu
13. test create/update menu
14. test inventory units/list/detail/create/update/history/delete
15. test low stock alerts
16. test kitchen complete
17. test refresh/logout
18. chạy các case lỗi
19. dùng file `03_postman_db_check_queries.sql` để soi DB

---

## 8. Hướng dẫn test chi tiết theo từng nhóm

## 8.1 Phase A - Chuẩn bị dữ liệu

### Bước A1. Reset DB

Chạy:

- `sql/01_reset_local_database.sql`

Kỳ vọng:

- database `goldenheart_restaurant` được tạo lại

### Bước A2. Start backend

Kỳ vọng:

- app start thành công
- Hibernate tạo bảng
- bootstrap tạo role và admin mặc định

### Bước A3. Seed dữ liệu mẫu

Chạy:

- `sql/02_seed_reference_data.sql`

Kỳ vọng:

- DB có branch, category, ingredient, inventory, menu item, order item mẫu

---

## 8.2 Phase B - Auth

### B1. Login admin

Request:

- `Auth / Login Admin`

Mục tiêu:

- lấy `adminAccessToken`

Kỳ vọng:

- status `200`
- response có `accessToken`
- Postman lưu `adminAccessToken`
- cookie jar có `refreshToken`

### B2. Login sai mật khẩu

Request:

- `Auth / Login Wrong Password`

Kỳ vọng:

- status `401`

### B3. Register customer tự do

Request:

- `Auth / Register Customer Self`

Mục tiêu:

- test luồng self-register của customer

Kỳ vọng:

- status `201`

### B4. Register trùng email

Request:

- `Auth / Register Customer Duplicate Email`

Kỳ vọng:

- status `409`

### B5. Refresh token

Request:

- `Auth / Refresh Current Session`

Điều kiện:

- đã login thành công trước đó trong cùng session Postman

Kỳ vọng:

- status `200`
- access token mới được trả về

### B6. Refresh không có cookie

Request:

- `Auth / Refresh Without Cookie`

Kỳ vọng:

- status `401`

### B7. Logout

Request:

- `Auth / Logout Current Session`

Kỳ vọng:

- status `200`
- refresh token bị revoke

---

## 8.3 Phase C - Roles và Employees

### C1. Lấy danh sách role

Request:

- `Roles / Get Roles`

Điều kiện:

- đã login admin

Kỳ vọng:

- status `200`
- Postman lưu `managerRoleId`, `staffRoleId`

### C2. Tạo manager bằng admin

Request:

- `Employees / Create Manager By Admin`

Kỳ vọng:

- status `201`
- Postman lưu `managerId`

### C3. Login manager

Request:

- `Auth / Login Manager`

Kỳ vọng:

- status `200`
- Postman lưu `managerAccessToken`

### C4. Tạo staff bằng admin

Request:

- `Employees / Create Staff By Admin`

Kỳ vọng:

- status `201`
- Postman lưu `staffId`

### C5. Login staff

Request:

- `Auth / Login Staff`

Kỳ vọng:

- status `200`
- Postman lưu `staffAccessToken`

### C6. Tạo trùng username

Request:

- `Employees / Create Employee Duplicate Username`

Kỳ vọng:

- status `409`

### C7. Manager tạo staff không truyền roleId

Request:

- `Employees / Manager Create Staff Without RoleId`

Kỳ vọng:

- status `201`
- user mới mặc định nhận role `STAFF`

### C8. Manager cố tình truyền roleId

Request:

- `Employees / Manager Create With RoleId Forbidden`

Kỳ vọng:

- status `403`

### C9. Admin xem danh sách nhân viên

Request:

- `Employees / Get Employees Admin`

Kỳ vọng:

- status `200`

### C10. Xem chi tiết nhân viên

Request:

- `Employees / Get Employee By Id`

Kỳ vọng:

- status `200`

### C11. Manager cập nhật thông tin nhân viên

Request:

- `Employees / Manager Update Employee Basic Info`

Kỳ vọng:

- status `200`

### C12. Manager cố cập nhật role

Request:

- `Employees / Manager Update Role Forbidden`

Kỳ vọng:

- status `403`

### C13. Staff xem hồ sơ cá nhân

Request:

- `Employees / Staff Get My Profile`

Kỳ vọng:

- status `200`
- response không lộ các trường nhạy cảm kiểu `salary`, `passwordHash`

### C14. Staff tự cập nhật hồ sơ

Request:

- `Employees / Staff Update My Profile`

Kỳ vọng:

- status `200`

### C15. Staff cố xem danh sách nhân viên

Request:

- `Employees / Staff Get Employees Forbidden`

Kỳ vọng:

- status `403`

### C16. Admin xóa nhân viên

Request:

- `Employees / Admin Delete Employee`

Kỳ vọng:

- status `200`
- đây là xóa mềm

### C17. Admin cố xóa chính mình

Request:

- `Employees / Admin Delete Self Forbidden`

Kỳ vọng:

- status `403`

---

## 8.4 Phase D - Customers

### D1. Tạo customer 01

Request:

- `Customers / Create Customer 01`

Kỳ vọng:

- status `201`
- Postman lưu `customerId`

### D2. Tạo customer 02

Request:

- `Customers / Create Customer 02`

Kỳ vọng:

- status `201`

### D3. Tạo customer trùng email

Request:

- `Customers / Create Customer Duplicate Email`

Kỳ vọng:

- status `409`

### D4. Lấy danh sách customer

Request:

- `Customers / Get Customers`

Kỳ vọng:

- status `200`

### D5. Lấy customer theo id

Request:

- `Customers / Get Customer By Id`

Kỳ vọng:

- status `200`

### D6. Cập nhật customer

Request:

- `Customers / Update Customer`

Kỳ vọng:

- status `200`

### D7. Xóa customer

Request:

- `Customers / Delete Customer`

Kỳ vọng:

- status `200`
- đây là xóa mềm

### D8. Lấy lại customer đã xóa

Request:

- `Customers / Get Deleted Customer 404`

Kỳ vọng:

- status `404`

---

## 8.5 Phase E - Menu và Recipe

### E1. Tạo menu item mới

Request:

- `Menu / Create Menu Item Pho Bo Chin`

Kỳ vọng:

- status `201`
- Postman lưu `createdMenuItemId`

Dữ liệu request hiện đang dùng:

- `branchId = 1`
- `categoryId = 1`
- ingredient:
  - `Beef`
  - `Rice Noodle`

### E2. Tạo menu trùng tên

Request:

- `Menu / Create Menu Duplicate Name Conflict`

Kỳ vọng:

- status `409`

### E3. Lấy danh sách menu

Request:

- `Menu / Get Menu Items`

Kỳ vọng:

- status `200`

### E4. Lấy menu theo id

Request:

- `Menu / Get Menu Item By Id`

Kỳ vọng:

- status `200`

### E5. Cập nhật menu seed sẵn

Request:

- `Menu / Update Seeded Menu Item`

Kỳ vọng:

- status `200`

Lưu ý:

- request này đang update `menuItemId = 1`
- món seed `Pho Bo Tai` sẽ được cập nhật lại recipe và giá

---

## 8.6 Phase F - Inventory, đơn vị tính, lịch sử, cảnh báo

### F1. Lấy danh sách đơn vị tính

Request:

- `Inventory / Get Measurement Units`

Kỳ vọng:

- status `200`
- Postman cập nhật lại các biến:
  - `kgUnitId`
  - `pieceUnitId`
  - `literUnitId`
  - `gramUnitId`
  - `milliliterUnitId`

### F2. Lấy danh sách inventory

Request:

- `Inventory / Get Inventory Items`

Kỳ vọng:

- status `200`

### F3. Lấy chi tiết inventory item

Request:

- `Inventory / Get Inventory Item By Id`

Kỳ vọng:

- status `200`

### F4. Tạo inventory item mới

Request:

- `Inventory / Create Inventory Item Chicken Egg As Admin`

Kỳ vọng:

- status `201`
- Postman lưu `createdInventoryId`

Ý nghĩa:

- request này vừa tạo nguyên liệu mới kiểu `Chicken Egg Test API`
- đồng thời tạo inventory record tương ứng

### F5. Lấy danh sách cảnh báo tồn kho thấp

Request:

- `Inventory / Get Low Stock Alerts`

Kỳ vọng:

- status `200`
- nếu item nào dưới `min_stock_level` thì response trả về danh sách alert + message tương ứng

### F6. Cập nhật inventory mới tạo về quantity = 0

Request:

- `Inventory / Update Created Inventory Item Set Quantity Zero`

Kỳ vọng:

- status `200`

Vì sao collection làm bước này:

- service đang chặn xóa inventory nếu `quantity > 0`
- nên request này là bước chuẩn bị trước khi xóa mềm

### F7. Xem lịch sử thay đổi inventory

Request:

- `Inventory / Get Created Inventory History`

Kỳ vọng:

- status `200`
- thấy được log tạo mới và log cập nhật số lượng

### F8. Xóa inventory item vừa tạo

Request:

- `Inventory / Delete Created Inventory Item`

Kỳ vọng:

- status `200`
- đây là xóa mềm

Lưu ý:

- request này nên chạy sau bước F6
- nếu bạn chưa đưa quantity về `0`, API có thể từ chối xóa theo đúng business rule

---

## 8.7 Phase G - Kitchen

### G1. Hoàn thành món bếp với order item seed sẵn

Request:

- `Kitchen / Complete Seeded Order Item As Admin`

Kỳ vọng:

- status `200`

API này sẽ:

- lấy `orderItemId = 1`
- đọc recipe của món
- kiểm tra tồn kho theo từng ingredient
- trừ kho
- ghi `stock_movements`
- cập nhật trạng thái `order_item`

Lưu ý rất quan trọng:

- request này là kiểu gần như one-shot
- nếu bạn đã complete `orderItemId = 1` rồi, chạy lại thường sẽ nhận `409`
- đó là đúng logic nghiệp vụ, không phải lỗi Postman

---

## 9. Ma trận quyền nhanh để đối chiếu khi test

### Auth

- public:
  - register
  - login
- cần refresh cookie:
  - refresh
  - logout

### Roles

- `ADMIN`, `MANAGER`: được xem

### Employees

- `ADMIN`: full mạnh nhất
- `MANAGER`: create/read/update, nhưng không delete và không được tự gán role tùy ý
- `STAFF`, `KITCHEN`: chỉ xem/sửa thông tin cá nhân

### Customers

- `ADMIN`, `MANAGER`: CRUD theo logic hiện tại
- `ADMIN`: có quyền xóa

### Menu

- `ADMIN`: create/update/delete
- `MANAGER`, `STAFF`, `KITCHEN`: chỉ get

### Inventory

- `ADMIN`, `MANAGER`: create/update/delete
- `STAFF`, `KITCHEN`: chỉ get

### Kitchen complete

- `ADMIN`, `KITCHEN`: được complete món

---

## 10. Những request nào nên chạy theo thứ tự cố định

Các request dưới đây nên chạy theo đúng thứ tự vì có phụ thuộc dữ liệu:

1. `Auth / Login Admin`
2. `Roles / Get Roles`
3. `Employees / Create Manager By Admin`
4. `Auth / Login Manager`
5. `Employees / Create Staff By Admin`
6. `Auth / Login Staff`
7. `Menu / Create Menu Item Pho Bo Chin`
8. `Inventory / Create Inventory Item Chicken Egg As Admin`
9. `Inventory / Update Created Inventory Item Set Quantity Zero`
10. `Inventory / Get Created Inventory History`
11. `Inventory / Delete Created Inventory Item`
12. `Kitchen / Complete Seeded Order Item As Admin`

Lý do:

- một số request sẽ tạo ra ID mới cho request sau
- một số request test đúng business rule, ví dụ phải update quantity về `0` rồi mới delete inventory

---

## 11. Kiểm tra DB sau khi test

Khi muốn soi DB nhanh sau một phase test, chạy:

- `sql/03_postman_db_check_queries.sql`

File này đã có sẵn các section:

- roles
- users_and_profiles
- refresh_tokens
- customers
- measurement_units
- ingredients_with_units
- inventory
- inventory_low_stock_alerts
- inventory_action_logs
- menu_items
- recipes
- orders_and_order_items
- stock_movements

Gợi ý:

- sau phase Auth: xem `refresh_tokens`
- sau phase Employees: xem `users_and_profiles`
- sau phase Customers: xem `customers`
- sau phase Menu: xem `menu_items`, `recipes`
- sau phase Inventory: xem `inventory`, `inventory_action_logs`, `inventory_low_stock_alerts`
- sau phase Kitchen: xem `orders_and_order_items`, `stock_movements`, `inventory`

---

## 12. Các lỗi thường gặp và cách hiểu đúng

### `401 Unauthorized`

Nguyên nhân thường gặp:

- access token sai
- token hết hạn
- refresh không có cookie

### `403 Forbidden`

Nguyên nhân thường gặp:

- đúng là bạn đã login, nhưng role không đủ quyền
- ví dụ staff đi gọi API list employee

### `404 Not Found`

Nguyên nhân thường gặp:

- record không tồn tại
- record đã bị xóa mềm

### `409 Conflict`

Nguyên nhân thường gặp:

- trùng email/username/customerCode
- complete lại order item đã hoàn thành
- tạo menu trùng tên trong phạm vi bị ràng buộc
- xóa inventory không hợp lệ theo business rule

### `500 Internal Server Error`

Nếu còn gặp `500`, nên kiểm tra theo thứ tự:

1. request body có đúng JSON không
2. environment variable có bị rỗng không
3. DB đã seed đúng theo file `02` chưa
4. terminal backend báo lỗi gì

---

## 13. Runbook nhanh nhất nếu bạn chỉ muốn test một mạch

Nếu muốn test nhanh toàn bộ luồng chính, làm đúng thứ tự này:

1. chạy `01_reset_local_database.sql`
2. start app
3. chạy `02_seed_reference_data.sql`
4. import collection + environment
5. chọn environment local
6. chạy `Auth / Login Admin`
7. chạy `Roles / Get Roles`
8. chạy `Employees / Create Manager By Admin`
9. chạy `Auth / Login Manager`
10. chạy `Employees / Create Staff By Admin`
11. chạy `Auth / Login Staff`
12. chạy `Employees / Staff Get My Profile`
13. chạy `Customers / Create Customer 01`
14. chạy `Customers / Get Customers`
15. chạy `Menu / Get Menu Items`
16. chạy `Menu / Create Menu Item Pho Bo Chin`
17. chạy `Menu / Update Seeded Menu Item`
18. chạy `Inventory / Get Measurement Units`
19. chạy `Inventory / Get Inventory Items`
20. chạy `Inventory / Create Inventory Item Chicken Egg As Admin`
21. chạy `Inventory / Update Created Inventory Item Set Quantity Zero`
22. chạy `Inventory / Get Created Inventory History`
23. chạy `Inventory / Delete Created Inventory Item`
24. chạy `Inventory / Get Low Stock Alerts`
25. chạy `Kitchen / Complete Seeded Order Item As Admin`
26. chạy `Auth / Refresh Current Session`
27. chạy `Auth / Logout Current Session`
28. chạy `03_postman_db_check_queries.sql` để kiểm tra DB

---

## 14. Kết luận

Ở trạng thái hiện tại, bạn không cần tạo dữ liệu tay để test các nhóm chính nữa. Chỉ cần:

1. reset DB
2. start app
3. seed file `02`
4. import Postman
5. test theo đúng thứ tự trong tài liệu này

Nếu về sau bạn thêm API mới, cách cập nhật guide tốt nhất là:

- thêm request vào collection trước
- thêm hoặc cập nhật biến trong environment nếu cần
- rồi mới cập nhật lại tài liệu này để tên request và thứ tự test luôn khớp 1:1 với Postman
