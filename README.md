# GoldenHeart Restaurant Backend

Backend của hệ thống quản lý nhà hàng GoldenHeart Restaurant. Dự án được xây dựng bằng Spring Boot, Spring Security JWT, Spring Data JPA và MySQL, phục vụ các nghiệp vụ vận hành nhà hàng như xác thực, nhân viên, chi nhánh, sơ đồ bàn, thực đơn, kho, POS, bếp, hóa đơn, thanh toán PayOS, báo cáo, khách hàng thân thiết và yêu cầu hủy hao.

## Mục Lục

- [Tổng Quan](#tổng-quan)
- [Công Nghệ Sử Dụng](#công-nghệ-sử-dụng)
- [Cấu Trúc Thư Mục](#cấu-trúc-thư-mục)
- [Yêu Cầu Môi Trường](#yêu-cầu-môi-trường)
- [Cấu Hình Ứng Dụng](#cấu-hình-ứng-dụng)
- [Khởi Tạo Database](#khởi-tạo-database)
- [Chạy Backend Local](#chạy-backend-local)
- [Build, Test Và Docker](#build-test-và-docker)
- [Xác Thực Và Phân Quyền](#xác-thực-và-phân-quyền)
- [Quy Ước API](#quy-ước-api)
- [Danh Sách Module Và Endpoint](#danh-sách-module-và-endpoint)
- [Tích Hợp Bên Ngoài](#tích-hợp-bên-ngoài)
- [Tài Liệu Và Công Cụ Kèm Theo](#tài-liệu-và-công-cụ-kèm-theo)
- [Quy Ước Phát Triển](#quy-ước-phát-triển)
- [Troubleshooting](#troubleshooting)

## Tổng Quan

Backend cung cấp REST API với prefix:

```text
http://localhost:1010/api/v1
```

Các nhóm nghiệp vụ chính:

- Đăng ký, đăng nhập, refresh token, đăng xuất, đổi mật khẩu và khôi phục mật khẩu bằng OTP.
- Quản lý tài khoản nhân viên, role, trạng thái người dùng và hồ sơ cá nhân.
- Quản lý chi nhánh, khu vực ăn uống, bàn nhà hàng, gộp bàn, tách bàn và trạng thái bàn.
- Quản lý danh mục, món ăn, công thức, ảnh món ăn và combo.
- Quản lý tồn kho, nhập kho, lịch sử điều chỉnh, cảnh báo tồn kho thấp và import Excel.
- Vận hành POS: tạo order, gán khách hàng, phục vụ món, tạo bill, thanh toán và tải hóa đơn PDF.
- Vận hành bếp: xem món đang chờ, cập nhật trạng thái chế biến và hoàn thành món.
- Tích hợp PayOS QR payment và webhook đồng bộ trạng thái giao dịch.
- Quản lý khách hàng, hạng thành viên, điểm tích lũy, coupon và review.
- Cung cấp dashboard và báo cáo doanh thu, phương thức thanh toán, trạng thái bill và dữ liệu theo thời gian.
- Quản lý yêu cầu hủy hao nguyên liệu có luồng tạo, duyệt, từ chối, thống kê và xuất báo cáo.

## Công Nghệ Sử Dụng

| Nhóm | Công nghệ |
| --- | --- |
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.5 |
| API | Spring Web MVC |
| Security | Spring Security, JWT, BCrypt |
| ORM | Spring Data JPA, Hibernate |
| Database | MySQL hoặc hệ tương thích MySQL như TiDB |
| Validation | Jakarta Bean Validation |
| Email | Spring Mail SMTP |
| Template/PDF | Thymeleaf, openhtmltopdf |
| Excel | Apache POI |
| Lưu ảnh | Cloudinary |
| Thanh toán | PayOS |
| Build | Gradle Wrapper |
| Container | Docker multi-stage build |
| Test | JUnit Platform, Spring Boot test starters |

## Cấu Trúc Thư Mục

```text
GoldenHeart-Restaurant/
|-- build.gradle
|-- settings.gradle
|-- Dockerfile
|-- gradlew
|-- gradlew.bat
|-- src/
|   |-- main/
|   |   |-- java/org/example/goldenheartrestaurant/
|   |   |   |-- GoldenHeartRestaurantApplication.java
|   |   |   |-- common/
|   |   |   |   |-- config/
|   |   |   |   |-- entity/
|   |   |   |   |-- exception/
|   |   |   |   |-- response/
|   |   |   |   `-- security/
|   |   |   `-- modules/
|   |   |       |-- auth/
|   |   |       |-- billing/
|   |   |       |-- combo/
|   |   |       |-- coupon/
|   |   |       |-- customer/
|   |   |       |-- customerportal/
|   |   |       |-- identity/
|   |   |       |-- inventory/
|   |   |       |-- menu/
|   |   |       |-- operations/
|   |   |       |-- order/
|   |   |       |-- paymentgateway/
|   |   |       |-- report/
|   |   |       |-- restaurant/
|   |   |       |-- review/
|   |   |       `-- waste/
|   |   `-- resources/
|   |       |-- application.properties
|   |       `-- templates/billing/bill-invoice.html
|   `-- test/
|-- postman/
|-- scripts/
|-- sql/
`-- ngrok/
```

Ý nghĩa các nhóm thư mục:

- `common/config`: cấu hình dùng chung như security, JWT, Cloudinary, PayOS, Jackson và PDF.
- `common/security`: filter JWT, service đọc user hiện tại và lớp user details.
- `common/exception`: exception nghiệp vụ và global exception handler.
- `common/response`: response wrapper và response phân trang.
- `modules/*/controller`: REST controller của từng domain.
- `modules/*/service`: xử lý nghiệp vụ, transaction và phân quyền theo role/branch.
- `modules/*/repository`: Spring Data JPA repository và projection.
- `modules/*/dto/request`: payload đầu vào của API.
- `modules/*/dto/response`: payload đầu ra của API.
- `modules/*/entity`: entity JPA ánh xạ bảng database.

## Yêu Cầu Môi Trường

Cần cài đặt:

- JDK 21.
- MySQL 8+ hoặc database tương thích MySQL.
- Docker nếu muốn build/chạy container.
- Postman hoặc Newman nếu muốn chạy bộ test API có sẵn.

Gradle không bắt buộc cài global vì repo đã có Gradle Wrapper.

Kiểm tra nhanh:

```powershell
java -version
.\gradlew.bat --version
```

## Cấu Hình Ứng Dụng

File cấu hình mặc định:

```text
src/main/resources/application.properties
```

Ứng dụng có import cấu hình local:

```properties
spring.config.import=optional:classpath:application-local.properties
```

Mỗi máy dev nên tạo file riêng:

```text
src/main/resources/application-local.properties
```

File `application-local.properties` đã được `.gitignore` bỏ qua, dùng để ghi đè cấu hình local và các giá trị nhạy cảm. Không commit mật khẩu database, mail, PayOS, Cloudinary hoặc JWT secret thật.

Mẫu cấu hình local:

```properties
server.port=1010

spring.datasource.url=jdbc:mysql://localhost:3306/goldenheart_restaurant?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=your_local_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

app.security.jwt.secret=replace-with-at-least-32-characters-secret
app.security.jwt.issuer=goldenheart-restaurant
app.security.jwt.access-token-expiration=15m
app.security.jwt.refresh-token-expiration=7d
app.security.jwt.refresh-cookie-name=refreshToken
app.security.jwt.refresh-cookie-secure=false
app.security.jwt.refresh-cookie-http-only=true
app.security.jwt.refresh-cookie-same-site=Strict
app.security.jwt.refresh-cookie-path=/api/v1/auth

app.bootstrap.admin.enabled=true
app.bootstrap.admin.username=admin
app.bootstrap.admin.password=Admin123
app.bootstrap.admin.email=admin@goldenheart.com
app.bootstrap.admin.full-name=System Admin

app.bootstrap.sample-data.enabled=false

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

app.password-recovery.email-from=your_email@gmail.com
app.password-recovery.dev-log-delivery=true

app.payos.enabled=false
app.payos.client-id=
app.payos.api-key=
app.payos.checksum-key=
app.payos.base-url=https://api-merchant.payos.vn
app.payos.return-url=http://localhost:5173/payment-success
app.payos.cancel-url=http://localhost:5173/payment-cancel
app.payos.webhook-url=

app.cloudinary.enabled=false
app.cloudinary.cloud-name=
app.cloudinary.api-key=
app.cloudinary.api-secret=
app.cloudinary.folder=goldenheart/menu-items

app.invoice.pdf-font-path=
```

Các biến môi trường quan trọng:

| Biến | Ý nghĩa |
| --- | --- |
| `JWT_SECRET` | Secret dùng để ký access token và refresh token. Nên dài tối thiểu 32 ký tự. |
| `PASSWORD_RECOVERY_EMAIL_FROM` | Email người gửi OTP khôi phục mật khẩu. |
| `PAYOS_ENABLED` | Bật hoặc tắt tích hợp PayOS. |
| `PAYOS_CLIENT_ID` | Client ID của PayOS. |
| `PAYOS_API_KEY` | API key của PayOS. |
| `PAYOS_CHECKSUM_KEY` | Checksum key dùng xác thực webhook PayOS. |
| `PAYOS_RETURN_URL` | URL frontend khi thanh toán thành công. |
| `PAYOS_CANCEL_URL` | URL frontend khi người dùng hủy thanh toán. |
| `PAYOS_WEBHOOK_URL` | Public webhook URL để PayOS gọi về backend. |
| `CLOUDINARY_ENABLED` | Bật hoặc tắt upload ảnh lên Cloudinary. |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name. |
| `CLOUDINARY_API_KEY` | Cloudinary API key. |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret. |
| `CLOUDINARY_FOLDER` | Thư mục lưu ảnh món ăn/combo. |
| `INVOICE_PDF_FONT_PATH` | Đường dẫn font Unicode tùy chọn cho hóa đơn PDF. |

## Khởi Tạo Database

Ứng dụng đang dùng:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Khi chạy lần đầu, Hibernate có thể tự tạo hoặc cập nhật schema dựa trên entity.

Các script SQL có sẵn:

```text
sql/01_reset_local_database.sql
sql/05_seed_full_test_data.sql
```

Quy trình khởi tạo đề xuất:

1. Tạo database local:

```sql
CREATE DATABASE goldenheart_restaurant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Cấu hình datasource trong `application-local.properties`.
3. Chạy backend để Hibernate tạo bảng.
4. Nếu cần dữ liệu demo, chạy seed trong `sql/05_seed_full_test_data.sql`.

Backend có bootstrap runner tự động:

- Tạo các role mặc định: `ADMIN`, `MANAGER`, `STAFF`, `KITCHEN`, `CUSTOMER`.
- Tạo tài khoản admin đầu tiên nếu `app.bootstrap.admin.enabled=true` và username chưa tồn tại.

Tài khoản bootstrap mặc định chỉ nên dùng local:

```text
username: admin
password: Admin123
```

## Chạy Backend Local

Từ thư mục backend:

```powershell
cd D:\GH\RM-BE\GoldenHeart-Restaurant
.\gradlew.bat bootRun
```

Trên Git Bash/Linux/macOS:

```bash
cd /path/to/RM-BE/GoldenHeart-Restaurant
./gradlew bootRun
```

Backend mặc định chạy tại:

```text
http://localhost:1010
```

Kiểm tra nhanh:

```powershell
curl http://localhost:1010/api/v1/public/menu/popular
```

Nếu API public trả JSON hoặc danh sách rỗng, backend đã chạy. Các endpoint protected sẽ trả `401` nếu chưa có JWT.

## Build, Test Và Docker

Chạy test:

```powershell
.\gradlew.bat test
```

Build jar:

```powershell
.\gradlew.bat build
```

Build bỏ qua test:

```powershell
.\gradlew.bat build -x test
```

Chạy jar sau khi build:

```powershell
java -jar build\libs\GoldenHeart-Restaurant-0.0.1-SNAPSHOT.jar
```

Build Docker image:

```powershell
docker build -t goldenheart-restaurant-be .
```

Chạy Docker container:

```powershell
docker run --rm -p 1010:1010 --env JWT_SECRET=replace-with-strong-secret goldenheart-restaurant-be
```

Khi chạy Docker thực tế, nên truyền datasource, mail, PayOS, Cloudinary và secret bằng biến môi trường hoặc secret manager của nền tảng deploy.

## Xác Thực Và Phân Quyền

Backend dùng Spring Security theo mô hình stateless:

- Người dùng đăng nhập bằng username/password.
- Backend trả `accessToken` trong JSON body.
- Backend set `refreshToken` trong HttpOnly cookie.
- Frontend gửi access token qua header:

```http
Authorization: Bearer <accessToken>
```

- Refresh token được lưu bằng hash trong bảng `refresh_tokens`.
- Refresh token cũ được revoke khi refresh hoặc logout.
- Đổi mật khẩu thành công sẽ revoke các refresh session đang hoạt động.

Endpoint public trong security:

| Endpoint | Mục đích |
| --- | --- |
| `POST /api/v1/auth/register` | Đăng ký tài khoản khách hàng. |
| `POST /api/v1/auth/login` | Đăng nhập. |
| `POST /api/v1/auth/refresh` | Cấp access token mới từ refresh cookie. |
| `POST /api/v1/auth/logout` | Đăng xuất và revoke refresh cookie. |
| `POST /api/v1/auth/password-recovery/**` | Khôi phục mật khẩu bằng OTP. |
| `POST /api/v1/payment-gateways/payos/webhook` | Webhook PayOS. |
| `GET /api/v1/public/**` | API public cho homepage/customer portal. |
| `OPTIONS /**` | CORS preflight. |

Các endpoint còn lại yêu cầu đăng nhập.

Role hệ thống:

| Role | Mô tả |
| --- | --- |
| `ADMIN` | Quản trị toàn hệ thống, nhân viên, chi nhánh, báo cáo và cấu hình nghiệp vụ. |
| `MANAGER` | Quản lý vận hành theo chi nhánh, dashboard, nhân viên, bàn, menu, kho, POS và bếp. |
| `STAFF` | Vận hành POS, bàn, order, thanh toán và một số thao tác kho/hủy hao theo nghiệp vụ. |
| `KITCHEN` | Vận hành màn hình bếp, món đang chờ, cập nhật trạng thái và hủy hao liên quan bếp. |
| `CUSTOMER` | Sử dụng customer portal: hồ sơ, đơn hàng, món đã dùng, điểm tích lũy, coupon và review. |

## Quy Ước API

Base URL:

```text
/api/v1
```

Response thành công dùng wrapper `ApiResponse`:

```json
{
  "success": true,
  "message": "OK",
  "data": {},
  "timestamp": "2026-08-11T20:00:00"
}
```

Response phân trang thường dùng `PageResponse`:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Response lỗi được chuẩn hóa bởi `GlobalExceptionHandler`:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "fieldName": "Error message"
  },
  "timestamp": "2026-08-11T20:00:00"
}
```

Quy ước request:

- JSON request dùng `Content-Type: application/json`.
- Upload ảnh/import Excel dùng `multipart/form-data`.
- API protected cần `Authorization: Bearer <accessToken>`.
- Refresh token nằm trong HttpOnly cookie nên client cần gửi cookie khi gọi API cross-origin.
- Filter phân trang thường dùng `page`, `size`, `keyword`, `branchId`, `status`, `dateFrom`, `dateTo`.

## Danh Sách Module Và Endpoint

### Auth

Base path: `/api/v1/auth`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `POST` | `/register` | Đăng ký tài khoản khách hàng. |
| `POST` | `/login` | Đăng nhập, trả access token và set refresh cookie. |
| `POST` | `/refresh` | Rotate refresh token và cấp access token mới. |
| `POST` | `/logout` | Đăng xuất và revoke refresh token. |
| `POST` | `/change-password` | Đổi mật khẩu cho user đã đăng nhập. |
| `POST` | `/admin/sync-customers` | Đồng bộ customer/user theo luồng admin. |
| `POST` | `/password-recovery/request-otp` | Gửi OTP khôi phục mật khẩu. |
| `POST` | `/password-recovery/verify-otp` | Xác thực OTP và trả reset token. |
| `POST` | `/password-recovery/reset-password` | Đặt mật khẩu mới bằng reset token. |

### Identity / Employees

Base path: `/api/v1/employees`, `/api/v1/roles`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/employees` | Danh sách nhân viên, hỗ trợ filter/phân trang. |
| `GET` | `/employees/{employeeId}` | Chi tiết nhân viên. |
| `POST` | `/employees` | Tạo nhân viên. |
| `PUT` | `/employees/{employeeId}` | Cập nhật nhân viên. |
| `DELETE` | `/employees/{employeeId}` | Xóa mềm nhân viên. |
| `GET` | `/employees/me` | Hồ sơ nhân viên đang đăng nhập. |
| `PUT` | `/employees/me` | Cập nhật hồ sơ cá nhân. |
| `GET` | `/roles` | Danh sách role. |

### Restaurant

Base path: `/api/v1/branches`, `/api/v1/dining-areas`, `/api/v1/tables`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/branches` | Danh sách chi nhánh. |
| `GET` | `/branches/{branchId}` | Chi tiết chi nhánh. |
| `POST` | `/branches` | Tạo chi nhánh. |
| `PUT` | `/branches/{branchId}` | Cập nhật chi nhánh. |
| `DELETE` | `/branches/{branchId}` | Xóa chi nhánh. |
| `GET` | `/dining-areas` | Danh sách khu vực ăn theo chi nhánh. |
| `POST` | `/dining-areas` | Tạo khu vực ăn. |
| `PUT` | `/dining-areas/{areaId}` | Cập nhật khu vực ăn. |
| `DELETE` | `/dining-areas/{areaId}` | Xóa khu vực ăn. |
| `GET` | `/tables` | Danh sách bàn theo chi nhánh/khu vực/trạng thái. |
| `GET` | `/tables/{tableId}` | Chi tiết bàn. |
| `POST` | `/tables` | Tạo bàn. |
| `PUT` | `/tables/{tableId}` | Cập nhật bàn. |
| `DELETE` | `/tables/{tableId}` | Xóa bàn. |
| `PUT` | `/tables/{tableId}/status` | Đổi trạng thái bàn. |
| `GET` | `/tables/{tableId}/active-order` | Lấy order đang mở của bàn. |
| `POST` | `/tables/{tableId}/split` | Tách order/bàn. |
| `POST` | `/tables/{tableId}/unmerge` | Hủy gộp bàn. |
| `POST` | `/tables/merge` | Gộp bàn. |

### Menu Và Combo

Base path: `/api/v1/categories`, `/api/v1/menu-items`, `/api/v1/combos`, `/api/v1/public`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/categories` | Danh sách danh mục. |
| `GET` | `/categories/{categoryId}` | Chi tiết danh mục. |
| `POST` | `/categories` | Tạo danh mục. |
| `PUT` | `/categories/{categoryId}` | Cập nhật danh mục. |
| `DELETE` | `/categories/{categoryId}` | Xóa danh mục. |
| `GET` | `/menu-items` | Danh sách món ăn, hỗ trợ filter/phân trang. |
| `GET` | `/menu-items/{menuItemId}` | Chi tiết món ăn. |
| `POST` | `/menu-items` | Tạo món ăn, hỗ trợ upload ảnh multipart. |
| `PUT` | `/menu-items/{menuItemId}` | Cập nhật món ăn, hỗ trợ upload ảnh multipart. |
| `DELETE` | `/menu-items/{menuItemId}` | Xóa món ăn. |
| `GET` | `/combos` | Danh sách combo. |
| `GET` | `/combos/{id}` | Chi tiết combo. |
| `POST` | `/combos` | Tạo combo multipart. |
| `PUT` | `/combos/{id}` | Cập nhật combo multipart. |
| `DELETE` | `/combos/{id}` | Xóa combo. |
| `GET` | `/public/menu/popular` | Danh sách món phổ biến cho homepage. |
| `GET` | `/public/menu-items/{menuItemId}/reviews` | Review public của món. |

### Customers, Loyalty Và Portal

Base path: `/api/v1/customers`, `/api/v1/customer-tiers`, `/api/v1/me`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/customers/lookup` | Tìm nhanh khách hàng theo keyword. |
| `GET` | `/customers` | Danh sách khách hàng. |
| `GET` | `/customers/{customerId}` | Chi tiết khách hàng. |
| `POST` | `/customers` | Tạo khách hàng. |
| `POST` | `/customers/quick-create` | Tạo nhanh khách hàng trong POS. |
| `PUT` | `/customers/{customerId}` | Cập nhật khách hàng. |
| `DELETE` | `/customers/{customerId}` | Xóa mềm khách hàng. |
| `GET` | `/customers/{customerId}/loyalty-transactions` | Lịch sử điểm của khách hàng. |
| `GET` | `/customer-tiers` | Danh sách hạng thành viên. |
| `GET` | `/customer-tiers/{tierId}` | Chi tiết hạng thành viên. |
| `POST` | `/customer-tiers` | Tạo hạng thành viên. |
| `PUT` | `/customer-tiers/{tierId}` | Cập nhật hạng thành viên. |
| `DELETE` | `/customer-tiers/{tierId}` | Xóa hạng thành viên. |
| `GET` | `/me/profile` | Customer xem hồ sơ. |
| `PUT` | `/me/profile` | Customer cập nhật hồ sơ. |
| `GET` | `/me/loyalty/transactions` | Customer xem lịch sử điểm. |
| `GET` | `/me/orders` | Customer xem lịch sử đơn. |
| `GET` | `/me/dishes-eaten` | Customer xem món đã dùng. |
| `POST` | `/me/reviews` | Customer tạo review. |
| `GET` | `/me/reviews` | Customer xem review của mình. |
| `GET` | `/me/coupons` | Customer xem coupon. |

### Inventory

Base path: `/api/v1/inventory`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/units` | Danh sách đơn vị đo. |
| `GET` | `/` | Danh sách hàng tồn kho. |
| `GET` | `/summary` | Tổng quan tồn kho. |
| `GET` | `/reports/movements` | Báo cáo biến động kho. |
| `GET` | `/alerts` | Cảnh báo tồn kho thấp/hết. |
| `GET` | `/import/template` | Tải file template import Excel. |
| `POST` | `/import/preview` | Preview file Excel trước khi import. |
| `POST` | `/import/commit` | Commit import Excel. |
| `GET` | `/{inventoryId}` | Chi tiết item kho. |
| `GET` | `/{inventoryId}/history` | Lịch sử điều chỉnh item. |
| `POST` | `/` | Tạo item kho. |
| `POST` | `/{inventoryId}/restock` | Nhập thêm tồn kho. |
| `PUT` | `/{inventoryId}` | Cập nhật item kho. |
| `DELETE` | `/{inventoryId}` | Xóa item kho. |

### Order, Kitchen, Billing Và Payment

Base path: `/api/v1/orders`, `/api/v1/kitchen`, `/api/v1/bills`, `/api/v1/payment-gateways`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `POST` | `/orders` | Tạo order cho bàn/POS. |
| `GET` | `/orders/{orderId}` | Chi tiết order. |
| `GET` | `/orders/{orderId}/bills` | Danh sách bill của order. |
| `PUT` | `/orders/{orderId}/customer` | Gán khách hàng vào order. |
| `PUT` | `/orders/order-items/{orderItemId}/serve` | Đánh dấu món đã phục vụ. |
| `GET` | `/kitchen/orders/pending` | Danh sách món đang chờ bếp. |
| `PUT` | `/kitchen/order-items/{orderItemId}/status` | Cập nhật trạng thái món bếp. |
| `POST` | `/kitchen/order-items/{orderItemId}/complete` | Hoàn thành món bếp. |
| `GET` | `/bills/preview` | Preview bill trước khi thanh toán. |
| `GET` | `/bills/history` | Lịch sử bill. |
| `GET` | `/bills/{billId}` | Chi tiết bill. |
| `GET` | `/bills/{billId}/invoice.pdf` | Tải hóa đơn PDF. |
| `POST` | `/bills` | Tạo bill. |
| `POST` | `/bills/{billId}/payments` | Ghi nhận thanh toán. |
| `POST` | `/bills/{billId}/payos/qr` | Tạo QR PayOS. |
| `GET` | `/bills/{billId}/payos/qr` | Lấy QR PayOS mới nhất của bill. |
| `POST` | `/bills/{billId}/payos/qr/cancel` | Hủy QR PayOS đang chờ. |
| `GET` | `/payment-gateways/transactions/{transactionId}` | Lấy trạng thái giao dịch payment gateway. |
| `POST` | `/payment-gateways/payos/webhook` | Webhook PayOS. |

### Report

Base path: `/api/v1/reports`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/dashboard` | Dữ liệu dashboard tổng hợp. |
| `GET` | `/revenue/summary` | Tổng hợp doanh thu theo khoảng thời gian. |
| `GET` | `/revenue/timeseries` | Chuỗi doanh thu theo ngày/tháng/năm. |
| `GET` | `/payments/method-breakdown` | Cơ cấu phương thức thanh toán. |
| `GET` | `/bills/status-summary` | Tổng hợp trạng thái bill. |

### Waste Requests

Base path: `/api/v1/waste-requests`

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `POST` | `/` | Tạo yêu cầu hủy hao multipart. |
| `GET` | `/` | Danh sách yêu cầu hủy hao. |
| `GET` | `/stats` | Thống kê hủy hao. |
| `GET` | `/export` | Xuất báo cáo hủy hao. |
| `GET` | `/{id}` | Chi tiết yêu cầu. |
| `PUT` | `/{id}/approve` | Duyệt yêu cầu. |
| `PUT` | `/{id}/reject` | Từ chối yêu cầu. |
| `GET` | `/pending-count` | Số yêu cầu đang chờ duyệt. |

## Tích Hợp Bên Ngoài

### PayOS

PayOS được dùng cho QR payment trong POS.

Luồng chính:

1. Frontend tạo bill hoặc lấy bill đang mở.
2. Frontend gọi `POST /api/v1/bills/{billId}/payos/qr`.
3. Backend tạo payment link/QR trên PayOS và lưu `PaymentGatewayTransaction`.
4. Frontend hiển thị QR, checkout URL và polling trạng thái transaction.
5. PayOS gọi webhook về `/api/v1/payment-gateways/payos/webhook`.
6. Backend verify checksum và đồng bộ trạng thái bill/payment.
7. Frontend có thể gọi lại transaction để refresh trạng thái.

Khi test webhook local, cần public URL như ngrok:

```powershell
.\scripts\start-payos-ngrok.ps1
```

Sau đó cập nhật `PAYOS_WEBHOOK_URL` bằng public URL do ngrok cung cấp.

### Cloudinary

Cloudinary được dùng cho ảnh món ăn/combo.

Nếu test local không cần upload cloud:

```properties
app.cloudinary.enabled=false
```

Nếu bật Cloudinary:

```properties
app.cloudinary.enabled=true
app.cloudinary.cloud-name=...
app.cloudinary.api-key=...
app.cloudinary.api-secret=...
app.cloudinary.folder=goldenheart/menu-items
```

### Email OTP

Password recovery dùng Spring Mail. Với Gmail cần App Password, không dùng mật khẩu đăng nhập Gmail thông thường.

Để test local không gửi email thật, có thể bật log delivery nếu service hỗ trợ cấu hình này:

```properties
app.password-recovery.dev-log-delivery=true
```

## Tài Liệu Và Công Cụ Kèm Theo

| File/Thư mục | Nội dung |
| --- | --- |
| `AUTH_SETUP.md` | Hướng dẫn riêng về JWT auth, refresh token và đổi mật khẩu. |
| `API_TESTING_RUNBOOK.md` | Runbook test API bằng Postman/Newman. |
| `PROJECT_STRUCTURE.md` | Ghi chú cấu trúc module backend. |
| `BE_ROADMAP.md` | Roadmap/công việc backend. |
| `postman/` | Postman collection và environment E2E. |
| `sql/` | Script reset và seed database local. |
| `ngrok/` | Cấu hình liên quan webhook PayOS/ngrok. |
| `scripts/` | Script tiện ích khi test PayOS/local. |

## Quy Ước Phát Triển

- Controller chỉ điều phối request/response, không nhồi logic nghiệp vụ lớn.
- Service xử lý nghiệp vụ, phân quyền, transaction và validate chéo.
- Repository chỉ truy vấn database, ưu tiên method rõ nghĩa hoặc projection.
- Không trả entity trực tiếp cho frontend nếu API đã có response DTO.
- Request DTO phải dùng validation annotation phù hợp.
- Response DTO chỉ trả trường an toàn, không trả password hash, token hash, secret hoặc thông tin nội bộ.
- Mỗi module nên giữ boundary riêng: `controller`, `service`, `repository`, `entity`, `dto`.
- Nhiều entity đang dùng soft delete với `deleted_at`; cần lưu ý khi kiểm tra duplicate và lookup.
- Các thao tác liên quan chi nhánh cần truyền/kiểm tra `branchId` để đúng phạm vi vận hành.
- Không commit `application-local.properties`, build output, log runtime hoặc secret.

## Troubleshooting

### Backend không kết nối được database

Kiểm tra:

- Database đã chạy chưa.
- URL JDBC đúng port/schema chưa.
- Username/password trong `application-local.properties`.
- MySQL có yêu cầu SSL hoặc `allowPublicKeyRetrieval` theo cấu hình đang dùng không.

### Frontend gọi API bị CORS hoặc mất cookie

Kiểm tra:

- Backend đang chạy port `1010`.
- Frontend dùng `VITE_API_BASE_URL=http://localhost:1010/api/v1` hoặc proxy `/api/v1`.
- Axios `withCredentials=true` đã bật trong frontend.
- Cookie refresh token path là `/api/v1/auth`.
- Nếu deploy HTTPS, cần cấu hình cookie secure/same-site phù hợp.

### Login thành công nhưng API protected bị 401

Kiểm tra:

- Frontend đã lưu `accessToken` vào localStorage chưa.
- Request có header `Authorization: Bearer <token>` không.
- `JWT_SECRET` lúc login và lúc verify có cùng giá trị không.
- Token đã hết hạn chưa.

### Không tạo được PDF hóa đơn tiếng Việt

Kiểm tra:

- Template `src/main/resources/templates/billing/bill-invoice.html`.
- Font Unicode trên máy chạy server.
- Thử cấu hình `INVOICE_PDF_FONT_PATH` trỏ đến font hỗ trợ tiếng Việt.

### PayOS webhook không cập nhật giao dịch

Kiểm tra:

- `PAYOS_WEBHOOK_URL` phải là public URL, không phải localhost.
- Checksum key đúng với merchant PayOS.
- Endpoint `/api/v1/payment-gateways/payos/webhook` không bị chặn.
- Ngrok tunnel còn hoạt động.

### Upload ảnh thất bại

Kiểm tra:

- `app.cloudinary.enabled`.
- Cloud name, API key, API secret.
- Giới hạn file `spring.servlet.multipart.max-file-size` và `app.cloudinary.max-file-size`.
- Request frontend có đúng `multipart/form-data`.
