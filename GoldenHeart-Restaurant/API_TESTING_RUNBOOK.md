# GoldenHeart Restaurant – API Testing Runbook

> **Base URL**: `http://localhost:1010`
> **Auth**: Bearer token (lấy từ login, gán vào Header `Authorization: Bearer <token>`)
> **Refresh Token**: Tự động đặt vào cookie `refreshToken` sau khi login
> **Seed Data**: Chạy file `sql/05_seed_full_test_data.sql` trước khi test

---

## Quy ước màu trạng thái kỳ vọng

| Kỳ vọng | HTTP Status |
|---|---|
| ✅ 200 OK | Thành công |
| ✅ 201 Created | Tạo mới thành công |
| ❌ 400 Bad Request | Input không hợp lệ |
| ❌ 401 Unauthorized | Chưa đăng nhập / token hết hạn |
| ❌ 403 Forbidden | Đã đăng nhập nhưng không đủ quyền |
| ❌ 404 Not Found | Resource không tồn tại |
| ❌ 409 Conflict | Vi phạm rule nghiệp vụ |

---

## Thứ tự test – Run Book

```
PHASE 1: AUTH & IDENTITY
  Step 1.1  → Login Admin
  Step 1.2  → Get my profile
  Step 1.3  → Create Staff (Q1)  ← mới
  Step 1.4  → Refresh token
  Step 1.5  → Logout

PHASE 2: MASTER DATA (dữ liệu nền)
  Step 2.1  → List Roles
  Step 2.2  → List Employees
  Step 2.3  → Get measurement units
  Step 2.4  → List inventory

PHASE 3: MENU & INVENTORY SETUP
  Step 3.1  → List menu items (Q1)
  Step 3.2  → Create menu item
  Step 3.3  → Create inventory item
  Step 3.4  → Update inventory item (điều chỉnh tồn)
  Step 3.5  → Get inventory alerts

PHASE 4: TABLE MANAGEMENT
  Step 4.1  → List tables (Q1)
  Step 4.2  → Get active order by table (trước khi có order)
  Step 4.3  → Update table status → RESERVED
  Step 4.4  → Reset table → AVAILABLE

PHASE 5: ORDER FLOW (luồng chính)
  Step 5.1  → Create Order (mở bàn T01 – Q1)
  Step 5.2  → Get Order by ID
  Step 5.3  → Add more items (gọi thêm món)
  Step 5.4  → Get active order by table

PHASE 6: KITCHEN FLOW
  Step 6.1  → Kitchen: get pending items
  Step 6.2  → Kitchen: start processing (PENDING → PROCESSING)
  Step 6.3  → Kitchen: complete item (PROCESSING → COMPLETED, trừ kho)

PHASE 7: SERVE & BILL
  Step 7.1  → Staff: serve order item (COMPLETED → SERVED)
  Step 7.2  → Create bill (tính tiền)
  Step 7.3  → Add payment (thanh toán)

PHASE 8: ADVANCED ORDER OPS
  Step 8.1  → Split table
  Step 8.2  → Merge tables

PHASE 9: CUSTOMER MANAGEMENT
  Step 9.1  → Create customer
  Step 9.2  → List customers
  Step 9.3  → Update customer

PHASE 10: ERROR CASES (negative tests)
  Step 10.1 → Login sai password
  Step 10.2 → Access protected route khi không có token
  Step 10.3 → Staff tạo bàn của branch khác
  Step 10.4 → Kitchen complete item khi thiếu tồn
  Step 10.5 → Tạo bill khi món chưa SERVED
```

---

## PHASE 1 – AUTH & IDENTITY

### Step 1.1 – Login Admin

**POST** `/api/v1/auth/login`

```json
{
  "username": "admin",
  "password": "Admin123"
}
```

**Kỳ vọng: 200 OK**
```json
{
  "message": "Login successfully",
  "data": {
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "username": "admin",
    "role": "ADMIN"
  }
}
```
> 🔑 **Lưu `accessToken`** vào Postman variable `{{adminToken}}`
> 🍪 Cookie `refreshToken` được đặt tự động

---

### Step 1.2 – Get My Profile

**GET** `/api/v1/employees/me`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK**
```json
{
  "data": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN",
    "fullName": "System Admin"
  }
}
```

---

### Step 1.3 – Login as Staff Q1 (để lấy token cho phase sau)

**POST** `/api/v1/auth/login`
```json
{
  "username": "staff_q1_a",
  "password": "GoldenHeart@2026"
}
```
> 🔑 Lưu token vào `{{staffQ1Token}}`

**POST** `/api/v1/auth/login`
```json
{
  "username": "kitchen_q1",
  "password": "GoldenHeart@2026"
}
```
> 🔑 Lưu token vào `{{kitchenQ1Token}}`

---

### Step 1.4 – Create Employee (Admin tạo Staff mới)

**POST** `/api/v1/employees`
**Header**: `Authorization: Bearer {{adminToken}}`

```json
{
  "username": "staff_q1_new",
  "password": "GoldenHeart@2026",
  "roleId": 3,
  "fullName": "Nhân Viên Mới",
  "employeeCode": "EMP-099",
  "email": "staff.new@goldenheart.com",
  "phone": "0909000099",
  "branchId": 1,
  "gender": "female",
  "hireDate": "2026-04-15",
  "salary": 8000000
}
```

**Kỳ vọng: 201 Created**

---

### Step 1.5 – Refresh Token

**POST** `/api/v1/auth/refresh`
*(Cookie `refreshToken` được gửi tự động)*

**Kỳ vọng: 200 OK** → accessToken mới

---

### Step 1.6 – Logout

**POST** `/api/v1/auth/logout`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK**
```json
{ "message": "Logout successfully" }
```

---

### ⚠️ Step 1.7 – Refresh sau Logout (Negative)

**POST** `/api/v1/auth/refresh`

**Kỳ vọng: 401 Unauthorized** (refresh token đã bị revoke)

---

## PHASE 2 – MASTER DATA

### Step 2.1 – List Roles

**GET** `/api/v1/roles`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK** – trả về 5 roles: ADMIN, MANAGER, STAFF, KITCHEN, CUSTOMER

---

### Step 2.2 – List Employees (phân trang)

**GET** `/api/v1/employees?page=0&size=10`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK** – danh sách 8+ nhân viên

**GET** `/api/v1/employees?keyword=manager`

**Kỳ vọng: 200 OK** – chỉ trả về manager_q1, manager_q7

---

### Step 2.3 – Get Measurement Units

**GET** `/api/v1/inventory/units`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK**
```json
{
  "data": [
    { "id": 1, "code": "KG", "name": "Kilogram", "symbol": "kg" },
    ...
  ]
}
```
> 🔑 Lưu `unitId` (kg=1, L=3, pcs=5) vào variables

---

### Step 2.4 – List Inventory (Branch 1)

**GET** `/api/v1/inventory?branchId=1&page=0&size=20`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK** – 17 items

---

## PHASE 3 – MENU & INVENTORY SETUP

### Step 3.1 – List Menu Items (Branch 1)

**GET** `/api/v1/menu-items?branchId=1&page=0&size=20`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK** – 10 items branch Q1

**GET** `/api/v1/menu-items?branchId=1&categoryId=1`

**Kỳ vọng: 200 OK** – Chỉ Món Chính

**GET** `/api/v1/menu-items?keyword=bò`

**Kỳ vọng: 200 OK** – Bò Lúc Lắc, Phở Bò...

---

### Step 3.2 – Get Menu Item by ID

**GET** `/api/v1/menu-items/1`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK** – Bò Lúc Lắc chi tiết

---

### Step 3.3 – Create Inventory Item (nguyên liệu mới)

**POST** `/api/v1/inventory`
**Header**: `Authorization: Bearer {{adminToken}}`

```json
{
  "branchId": 1,
  "ingredientName": "Cà Chua",
  "unitId": 1,
  "quantity": 20.00,
  "minStockLevel": 2.00,
  "reorderLevel": 5.00,
  "averageUnitCost": 15000,
  "note": "Nhập kho lần đầu"
}
```

**Kỳ vọng: 201 Created**

> ⚠️ **Tạo lần 2 với cùng branchId + ingredientName → Kỳ vọng: 409 Conflict**

---

### Step 3.4 – Update Inventory (điều chỉnh tồn)

**PUT** `/api/v1/inventory/1`
**Header**: `Authorization: Bearer {{adminToken}}`

```json
{
  "quantity": 45.00,
  "minStockLevel": 5.00,
  "reorderLevel": 10.00,
  "note": "Kiểm kê định kỳ"
}
```

**Kỳ vọng: 200 OK** – quantity cập nhật, stock movement được ghi

---

### Step 3.5 – Get Inventory Alerts

**GET** `/api/v1/inventory/alerts?branchId=1`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK** – danh sách items dưới minStockLevel (nếu có)

---

### Step 3.6 – Get Inventory History

**GET** `/api/v1/inventory/1/history?page=0&size=10`
**Header**: `Authorization: Bearer {{adminToken}}`

**Kỳ vọng: 200 OK** – lịch sử thay đổi kho item #1

---

## PHASE 4 – TABLE MANAGEMENT

### Step 4.1 – List Tables (Branch 1)

**GET** `/api/v1/tables?branchId=1`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

**Kỳ vọng: 200 OK** – 5 bàn branch Q1

**GET** `/api/v1/tables?branchId=1&status=AVAILABLE`

**Kỳ vọng: 200 OK** – chỉ bàn AVAILABLE (T01–T04)

---

### Step 4.2 – Get Active Order by Table (trước khi có order)

**GET** `/api/v1/tables/1/active-order`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

**Kỳ vọng: 200 OK, data = null** (chưa có order)

---

### Step 4.3 – Update Table Status → RESERVED

**PUT** `/api/v1/tables/2/status`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

```json
{ "status": "RESERVED" }
```

**Kỳ vọng: 200 OK** – bàn T02 thành RESERVED

---

### Step 4.4 – Reset Table RESERVED → AVAILABLE

**PUT** `/api/v1/tables/2/status`
```json
{ "status": "AVAILABLE" }
```

**Kỳ vọng: 200 OK**

> ⚠️ **Thử đổi sang OCCUPIED trực tiếp → Kỳ vọng: 409 Conflict**
> (OCCUPIED chỉ được đặt qua order workflow)

---

## PHASE 5 – ORDER FLOW

### Step 5.1 – Create Order (Mở bàn T01 – Q1, gọi 2 món)

**POST** `/api/v1/orders`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

```json
{
  "tableId": 1,
  "branchId": 1,
  "customerId": 1,
  "items": [
    { "menuItemId": 1, "quantity": 2, "note": "ít cay" },
    { "menuItemId": 7, "quantity": 1, "note": null }
  ]
}
```

**Kỳ vọng: 201 Created**
```json
{
  "data": {
    "id": 1,
    "status": "PENDING",
    "tableId": 1,
    "tableNumber": "T01",
    "orderItems": [...]
  }
}
```
> 🔑 Lưu `orderId` → `{{orderId1}}`, `orderItemId` của từng item → `{{orderItemId1}}`, `{{orderItemId7}}`

---

### Step 5.2 – Confirm Table is OCCUPIED

**GET** `/api/v1/tables?branchId=1&status=OCCUPIED`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

**Kỳ vọng: 200 OK** – T01 có status OCCUPIED

---

### Step 5.3 – Get Order by ID

**GET** `/api/v1/orders/{{orderId1}}`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

**Kỳ vọng: 200 OK** – đầy đủ order + items

---

### Step 5.4 – Add More Items (gọi thêm món vào order đang mở)

**POST** `/api/v1/orders`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

```json
{
  "tableId": 1,
  "branchId": 1,
  "items": [
    { "menuItemId": 3, "quantity": 1, "note": "cho nhiều tôm" }
  ]
}
```

**Kỳ vọng: 201 Created** – order cũ được append thêm item mới (cùng tableId)

---

### Step 5.5 – Get Active Order by Table

**GET** `/api/v1/tables/1/active-order`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

**Kỳ vọng: 200 OK** – trả về order đang active của T01

---

## PHASE 6 – KITCHEN FLOW

> Dùng token `{{kitchenQ1Token}}`

### Step 6.1 – Kitchen: Get Pending Items

**GET** `/api/v1/kitchen/orders/pending?branchId=1`
**Header**: `Authorization: Bearer {{kitchenQ1Token}}`

**Kỳ vọng: 200 OK** – danh sách items đang PENDING / WAITING_STOCK / PROCESSING

---

### Step 6.2 – Kitchen: Start Processing (PENDING → PROCESSING)

**PUT** `/api/v1/kitchen/order-items/{{orderItemId1}}/status`
**Header**: `Authorization: Bearer {{kitchenQ1Token}}`

```json
{
  "status": "PROCESSING",
  "reason": null
}
```

**Kỳ vọng: 200 OK** – deduct kho ingredient theo recipe
```json
{
  "data": {
    "previousStatus": "PENDING",
    "status": "PROCESSING",
    "deductions": [
      { "ingredientName": "Thịt bò", "quantityDeducted": 0.40 },
      { "ingredientName": "Hành tây", "quantityDeducted": 0.10 }
    ]
  }
}
```

---

### Step 6.3 – Kitchen: Complete Item (PROCESSING → COMPLETED)

**POST** `/api/v1/kitchen/order-items/{{orderItemId1}}/complete`
**Header**: `Authorization: Bearer {{kitchenQ1Token}}`

**Kỳ vọng: 200 OK** – item xuất kho lần 2 (theo KitchenProductionService) hoặc chỉ đổi status

> ⚠️ **Lưu ý quan trọng**: Hiện tại dự án có **2 service xử lý COMPLETE**:
> - `KitchenWorkflowService.changeOrderItemStatus()` (dùng qua `/status` endpoint) – trừ kho ở bước PROCESSING
> - `KitchenProductionService.completeOrderItem()` (dùng qua `/complete` endpoint) – trừ kho ở bước COMPLETE
>
> ⚠️ **BUG**: `/complete` endpoint sẽ trừ kho **lần 2** nếu đã gọi `/status → PROCESSING` trước đó!
> Xem phần **DANH SÁCH LỖI** bên dưới.

---

### Step 6.4 – Kitchen: Cancel Item

**PUT** `/api/v1/kitchen/order-items/{{orderItemId7}}/status`
**Header**: `Authorization: Bearer {{kitchenQ1Token}}`

```json
{
  "status": "CANCELLED",
  "reason": "Hết nguyên liệu Phở"
}
```

**Kỳ vọng: 200 OK** – item cancelled, order recalculated

---

## PHASE 7 – SERVE & BILL

### Step 7.1 – Staff: Serve Completed Item

**PUT** `/api/v1/orders/order-items/{{orderItemId1}}/serve`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

**Kỳ vọng: 200 OK** – COMPLETED → SERVED

> ⚠️ **Tất cả items phải là SERVED** trước khi tạo bill được.

---

### Step 7.2 – Create Bill

**POST** `/api/v1/bills`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

```json
{
  "orderId": 1,
  "taxRate": 10,
  "discount": 20000,
  "paymentMethod": "CASH",
  "paidAmount": 0
}
```

**Kỳ vọng: 201 Created**
```json
{
  "data": {
    "id": 1,
    "status": "UNPAID",
    "subtotal": 567000,
    "tax": 56700,
    "discount": 20000,
    "total": 603700,
    "paidAmount": 0,
    "remainingAmount": 603700
  }
}
```
> 🔑 Lưu `billId` → `{{billId1}}`

---

### Step 7.3 – Add Payment (thanh toán 1 phần)

**POST** `/api/v1/bills/{{billId1}}/payments`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

```json
{
  "amount": 300000,
  "method": "CASH"
}
```

**Kỳ vọng: 200 OK** – status = PARTIAL

---

### Step 7.4 – Add Payment (thanh toán đủ)

**POST** `/api/v1/bills/{{billId1}}/payments`

```json
{
  "amount": 303700,
  "method": "CARD"
}
```

**Kỳ vọng: 200 OK** – status = PAID, order → COMPLETED, table → CLEANING

---

### Step 7.5 – Reset Table from CLEANING to AVAILABLE

**PUT** `/api/v1/tables/1/status`
**Header**: `Authorization: Bearer {{staffQ1Token}}`
```json
{ "status": "AVAILABLE" }
```

**Kỳ vọng: 200 OK**

---

## PHASE 8 – ADVANCED ORDER OPS

> Cần tạo 2 order trên 2 bàn khác nhau trước khi split/merge

### Step 8.1 – Chuẩn bị: Tạo order T02 với nhiều món

**POST** `/api/v1/orders`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

```json
{
  "tableId": 2,
  "branchId": 1,
  "items": [
    { "menuItemId": 1, "quantity": 3 },
    { "menuItemId": 2, "quantity": 2 },
    { "menuItemId": 3, "quantity": 1 }
  ]
}
```
> 🔑 Lưu `{{orderId2}}`, `{{orderItemId_T02_1}}`, `{{orderItemId_T02_2}}`

---

### Step 8.2 – Split Table (T02 → T03)

**POST** `/api/v1/tables/2/split`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

```json
{
  "targetTableId": 3,
  "items": [
    { "orderItemId": "{{orderItemId_T02_2}}", "quantity": 1 }
  ]
}
```

**Kỳ vọng: 200 OK**
```json
{
  "data": {
    "action": "SPLIT",
    "sourceTableId": 2,
    "targetTableId": 3,
    "sourceOrderStatus": "PENDING",
    "targetOrderStatus": "PENDING"
  }
}
```

> ⚠️ **T03 phải là AVAILABLE trước khi split**

---

### Step 8.3 – Merge Tables (T02 → T03)

*(Sau khi split, merge lại để test)*

**POST** `/api/v1/tables/merge`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

```json
{
  "sourceTableId": 2,
  "targetTableId": 3
}
```

**Kỳ vọng: 200 OK** – items của T02 được chuyển sang T03, T02 AVAILABLE

---

## PHASE 9 – CUSTOMER MANAGEMENT

### Step 9.1 – Create Customer

**POST** `/api/v1/customers`
**Header**: `Authorization: Bearer {{adminToken}}`

```json
{
  "name": "Võ Thị Bích",
  "phone": "0977888555",
  "email": "bich.vo@email.com",
  "address": "Q.Phú Nhuận"
}
```

**Kỳ vọng: 201 Created**

---

### Step 9.2 – List Customers

**GET** `/api/v1/customers?page=0&size=10`
**Header**: `Authorization: Bearer {{adminToken}}`

**GET** `/api/v1/customers?keyword=Minh`

**Kỳ vọng: 200 OK**

---

### Step 9.3 – Update Customer

**PUT** `/api/v1/customers/1`
**Header**: `Authorization: Bearer {{adminToken}}`

```json
{
  "phone": "0912345699",
  "address": "Q.1, TP.HCM – đã cập nhật"
}
```

**Kỳ vọng: 200 OK**

---

## PHASE 10 – NEGATIVE TESTS (Kiểm tra bảo vệ)

### Step 10.1 – Login sai password

**POST** `/api/v1/auth/login`
```json
{ "username": "admin", "password": "WrongPass" }
```

**Kỳ vọng: 401 Unauthorized**
```json
{ "message": "Username or password is incorrect" }
```

---

### Step 10.2 – Truy cập không có token

**GET** `/api/v1/employees`  *(không có Authorization header)*

**Kỳ vọng: 401 Unauthorized**

---

### Step 10.3 – Staff Q1 cố xem bàn của Q7

**GET** `/api/v1/tables?branchId=2`
**Header**: `Authorization: Bearer {{staffQ1Token}}`

**Kỳ vọng: 403 Forbidden**

---

### Step 10.4 – Tạo Bill khi còn item chưa SERVED

*(Tạo order mới, không serve item)*
**POST** `/api/v1/bills`
```json
{ "orderId": <orderId_chua_serve> }
```

**Kỳ vọng: 409 Conflict**
```json
{ "message": "Order can only be checked out after all dishes are served" }
```

---

### Step 10.5 – Thanh toán vượt số tiền còn lại

**POST** `/api/v1/bills/{{billId1}}/payments`
```json
{ "amount": 9999999, "method": "CASH" }
```

**Kỳ vọng: 409 Conflict**
```json
{ "message": "Payment amount cannot exceed the remaining bill amount" }
```

---

### Step 10.6 – Manager cố đổi role của nhân viên

**PUT** `/api/v1/employees/4`
**Header**: `Authorization: Bearer <managerToken>`
```json
{ "roleId": 1 }
```

**Kỳ vọng: 403 Forbidden**
```json
{ "message": "Manager cannot change employee role" }
```

---

### Step 10.7 – Kitchen cố complete item mà thiếu kho

*(Trước đó điều chỉnh tồn về 0)*
**PUT** `/api/v1/inventory/<inventoryId>`
```json
{ "quantity": 0.00 }
```
Sau đó gọi:
**PUT** `/api/v1/kitchen/order-items/<itemId>/status`
```json
{ "status": "PROCESSING" }
```

**Kỳ vọng**: Item chuyển → `WAITING_STOCK` (không throw lỗi 409, tự xử lý gracefully)

---

## 🐛 DANH SÁCH API CÓ VẤN ĐỀ LOGIC

### Bug 1 – Double Stock Deduction (nghiêm trọng ⚠️)

**Mô tả**: Khi kitchen dùng flow:
1. `PUT /kitchen/order-items/{id}/status` với `"status": "PROCESSING"` → trừ kho lần 1
2. `POST /kitchen/order-items/{id}/complete` → trừ kho **lần 2**

**Root cause**: `KitchenWorkflowService` trừ kho khi PROCESSING. `KitchenProductionService` trừ kho khi COMPLETE. Cả 2 service song song nhau, dùng bởi 2 endpoint khác nhau.

**Fix đề xuất**:
- **Chọn 1 trong 2 luồng**:
  - *Luồng A*: Trừ kho khi `PROCESSING` (như `KitchenWorkflowService` đang làm) → bỏ `KitchenProductionService` hoặc để `/complete` chỉ đổi status, không trừ kho
  - *Luồng B*: Trừ kho khi `COMPLETED` (như `KitchenProductionService` đang làm) → bỏ logic trừ kho trong `startProcessing()` của `KitchenWorkflowService`

**Khuyến nghị**: Chọn **Luồng A** (trừ kho khi PROCESSING) vì:
- Kitchen bắt đầu chế biến mới thực sự "tiêu thụ" nguyên liệu
- Có thể rollback nếu CANCEL trước khi COMPLETE

---

### Bug 2 – `/complete` endpoint không validate transition (trung bình ⚠️)

**Mô tả**: `POST /kitchen/order-items/{id}/complete` gọi `KitchenWorkflowService.completeOrderItem()` → gọi `changeOrderItemStatus()` với `COMPLETED`. Nhưng `changeOrderItemStatus()` validate transition từ PENDING → COMPLETED sẽ throw `ConflictException: "Unsupported kitchen status transition"` (vì chỉ cho phép PROCESSING → COMPLETED).

**Fix**: Item phải ở trạng thái `PROCESSING` hoặc `WAITING_STOCK` trước khi gọi `/complete`.

---

### Bug 3 – Recipe trùng ingredient cho một menu item (nhỏ)

**Mô tả**: File SQL seed có entry:
```sql
(13, 24, 0.20),  -- ingredient_id=24 không tồn tại
(13, 11, 0.20),  -- bánh phở
```
ingredient_id = 24 không tồn tại trong bảng ingredients → FK violation.

**Fix**: File `05_seed_full_test_data.sql` đã sửa, chỉ dùng ingredient_id hợp lệ.

---

### Bug 4 – BillingService yêu cầu tất cả items SERVED (có thể quá cứng)

**Mô tả**: `ensureOrderReadyForCheckout()` throw lỗi nếu bất kỳ item nào chưa SERVED, kể cả item `WAITING_STOCK` hoặc `PROCESSING`.

**Trong thực tế**: Nhà hàng có thể hủy món đang chờ bếp và vẫn muốn tính tiền phần đã bưng.

**Fix đề xuất**: Chỉ block nếu còn item ở `PROCESSING` (đã bắt đầu làm), cho phép checkout khi mọi billable item đều là SERVED hoặc CANCELLED.

---

### Bug 5 – RoleController thiếu `@GetMapping` (nhỏ)

**Kiểm tra**: Xem `RoleController.java` có expose endpoint `GET /api/v1/roles` không – nếu không thì cần thêm.

---

## Postman Variables Template

```js
// Khai báo trong Postman Collection Variables:
baseUrl    = "http://localhost:1010"
adminToken = ""  // fill sau login
staffQ1Token  = ""
kitchenQ1Token = ""
orderId1   = ""
billId1    = ""
orderItemId1 = ""
```

---

## Thứ tự chạy SQL trước khi test

```bash
# 1. Reset DB
mysql -u root -p1409 < sql/01_reset_local_database.sql

# 2. Chạy seed data
mysql -u root -p1409 goldenheart_restaurant < sql/05_seed_full_test_data.sql

# 3. Khởi động app (roles + admin được bootstrap tự động)
./gradlew bootRun
```

> **Lưu ý**: `AuthBootstrapRunner` sẽ seed roles và admin khi app khởi động nên file SQL dùng `INSERT IGNORE` để tránh trùng.
