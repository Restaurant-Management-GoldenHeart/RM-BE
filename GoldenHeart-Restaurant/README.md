# GoldenHeart Restaurant Backend

Backend cua he thong quan ly nha hang GoldenHeart Restaurant. Du an duoc xay dung bang Spring Boot, Spring Security JWT, Spring Data JPA va MySQL, phuc vu cac nghiep vu chinh cua nha hang: xac thuc, nhan vien, chi nhanh, so do ban, thuc don, combo, kho, POS, bep, hoa don, thanh toan PayOS, bao cao, khach hang than thiet va yeu cau huy hao.

## Muc Luc

- [Tong Quan](#tong-quan)
- [Cong Nghe Su Dung](#cong-nghe-su-dung)
- [Cau Truc Thu Muc](#cau-truc-thu-muc)
- [Yeu Cau Moi Truong](#yeu-cau-moi-truong)
- [Cau Hinh Ung Dung](#cau-hinh-ung-dung)
- [Khoi Tao Database](#khoi-tao-database)
- [Chay Backend Local](#chay-backend-local)
- [Build, Test Va Docker](#build-test-va-docker)
- [Xac Thuc Va Phan Quyen](#xac-thuc-va-phan-quyen)
- [Quy Uoc API](#quy-uoc-api)
- [Danh Sach Module Va Endpoint](#danh-sach-module-va-endpoint)
- [Tich Hop Ben Ngoai](#tich-hop-ben-ngoai)
- [Tai Lieu Va Cong Cu Kem Theo](#tai-lieu-va-cong-cu-kem-theo)
- [Quy Uoc Phat Trien](#quy-uoc-phat-trien)
- [Troubleshooting](#troubleshooting)

## Tong Quan

Backend chay REST API tai prefix:

```text
http://localhost:1010/api/v1
```

Vai tro cua backend:

- Quan ly dang ky, dang nhap, refresh token, logout, doi mat khau va khoi phuc mat khau bang OTP.
- Quan ly tai khoan nhan vien, role, trang thai nguoi dung va thong tin ca nhan.
- Quan ly chi nhanh, khu vuc an, ban nha hang, gop ban, tach ban va chuyen trang thai ban.
- Quan ly danh muc mon, mon an, cong thuc, anh mon an va combo.
- Quan ly ton kho, nhap kho, lich su dieu chinh, canh bao ton kho thap va import Excel.
- Van hanh POS: tao order, gan khach hang, phuc vu mon, tao bill, thanh toan va tai hoa don PDF.
- Van hanh bep: xem mon dang cho, cap nhat trang thai che bien va hoan thanh mon.
- Tich hop PayOS QR payment va webhook dong bo trang thai giao dich.
- Quan ly khach hang, hang thanh vien, diem tich luy, lich su giao dich, coupon va review.
- Cung cap dashboard/bao cao doanh thu, phuong thuc thanh toan, trang thai bill va timeseries.
- Quan ly yeu cau huy hao/xuat huy nguyen lieu co anh dinh kem va luong duyet.

## Cong Nghe Su Dung

| Nhom | Cong nghe |
| --- | --- |
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.5 |
| API | Spring Web MVC |
| Security | Spring Security, JWT, BCrypt |
| ORM | Spring Data JPA, Hibernate |
| Database | MySQL/TiDB compatible |
| Validation | Jakarta Bean Validation |
| Email | Spring Mail SMTP |
| Template/PDF | Thymeleaf, openhtmltopdf |
| Excel | Apache POI |
| Storage anh | Cloudinary |
| Payment | PayOS |
| Build | Gradle Wrapper |
| Container | Docker multi-stage build |
| Test | JUnit Platform, Spring Boot test starters |

## Cau Truc Thu Muc

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

Quy uoc module:

- `controller`: lop REST controller, nhan request va tra response DTO.
- `service`: xu ly nghiep vu, transaction, phan quyen theo role/branch.
- `repository`: Spring Data JPA repository va projection.
- `dto/request`: payload dau vao cua API.
- `dto/response`: payload dau ra cua API.
- `entity`: entity JPA gan voi bang database.
- `mapper`: chuyen entity sang DTO neu module can mapper rieng.

## Yeu Cau Moi Truong

Can cai dat:

- JDK 21.
- MySQL 8+ hoac database MySQL-compatible nhu TiDB.
- Gradle khong bat buoc cai global vi repo co Gradle Wrapper.
- Docker neu muon build image.
- Postman/Newman neu muon chay collection API.

Kiem tra nhanh:

```powershell
java -version
.\gradlew.bat --version
```

## Cau Hinh Ung Dung

File cau hinh mac dinh:

```text
src/main/resources/application.properties
```

Ung dung co dong sau:

```properties
spring.config.import=optional:classpath:application-local.properties
```

Vi vay moi may dev nen tao file rieng:

```text
src/main/resources/application-local.properties
```

File nay da duoc `.gitignore` bo qua, dung de ghi de cau hinh local va secret. Khong dua mat khau database, mail, PayOS, Cloudinary hoac JWT secret that vao README, commit hoac chat.

Mau cau hinh local:

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

Cac bien moi truong quan trong co the override:

| Bien | Y nghia |
| --- | --- |
| `JWT_SECRET` | Secret ky access token va refresh token. Nen dai toi thieu 32 ky tu. |
| `PASSWORD_RECOVERY_EMAIL_FROM` | Email nguoi gui OTP. |
| `PAYOS_ENABLED` | Bat/tat PayOS. |
| `PAYOS_CLIENT_ID` | Client ID PayOS. |
| `PAYOS_API_KEY` | API key PayOS. |
| `PAYOS_CHECKSUM_KEY` | Checksum key de verify webhook PayOS. |
| `PAYOS_RETURN_URL` | URL FE khi thanh toan thanh cong. |
| `PAYOS_CANCEL_URL` | URL FE khi huy thanh toan. |
| `PAYOS_WEBHOOK_URL` | Public webhook URL cho PayOS goi ve backend. |
| `CLOUDINARY_ENABLED` | Bat/tat upload anh Cloudinary. |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name. |
| `CLOUDINARY_API_KEY` | Cloudinary API key. |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret. |
| `CLOUDINARY_FOLDER` | Thu muc luu anh mon/combo. |
| `INVOICE_PDF_FONT_PATH` | Duong dan font Unicode tuy chon cho file hoa don PDF. |

## Khoi Tao Database

Ung dung dang dung JPA `ddl-auto=update`, nen khi chay lan dau Hibernate co the tao/cap nhat schema tu entity.

Cac script SQL co san:

```text
sql/01_reset_local_database.sql
sql/05_seed_full_test_data.sql
```

Luong khoi tao de nghi:

1. Tao database local:

```sql
CREATE DATABASE goldenheart_restaurant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Cau hinh datasource trong `application-local.properties`.
3. Chay backend de Hibernate tao bang.
4. Neu can data demo, chay seed trong `sql/05_seed_full_test_data.sql`.

Backend co bootstrap runner:

- Tao cac role mac dinh: `ADMIN`, `MANAGER`, `STAFF`, `KITCHEN`, `CUSTOMER`.
- Tao tai khoan admin dau tien neu `app.bootstrap.admin.enabled=true` va username chua ton tai.

Tai khoan bootstrap mac dinh nen chi dung local:

```text
username: admin
password: Admin123
```

## Chay Backend Local

Tu thu muc backend:

```powershell
cd D:\GH\RM-BE\GoldenHeart-Restaurant
.\gradlew.bat bootRun
```

Neu dung Git Bash/Linux/macOS:

```bash
cd /path/to/RM-BE/GoldenHeart-Restaurant
./gradlew bootRun
```

Backend mac dinh lang nghe tai:

```text
http://localhost:1010
```

Kiem tra nhanh:

```powershell
curl http://localhost:1010/api/v1/public/menu/popular
```

Neu endpoint public tra JSON hoac danh sach rong la ung dung da len. Cac endpoint protected se tra `401` neu chua co JWT.

## Build, Test Va Docker

Chay test:

```powershell
.\gradlew.bat test
```

Build jar:

```powershell
.\gradlew.bat build
```

Build bo qua test:

```powershell
.\gradlew.bat build -x test
```

Chay jar sau khi build:

```powershell
java -jar build\libs\GoldenHeart-Restaurant-0.0.1-SNAPSHOT.jar
```

Build Docker image:

```powershell
docker build -t goldenheart-restaurant-be .
```

Chay Docker container:

```powershell
docker run --rm -p 1010:1010 --env JWT_SECRET=replace-with-strong-secret goldenheart-restaurant-be
```

Khi chay Docker that, nen truyen day du datasource, mail, PayOS, Cloudinary bang bien moi truong hoac secret manager cua platform deploy.

## Xac Thuc Va Phan Quyen

Backend dung Spring Security theo mo hinh stateless:

- Login bang username/password.
- Backend tra `accessToken` trong JSON body.
- Backend set `refreshToken` trong HttpOnly cookie.
- Frontend gui access token qua header:

```http
Authorization: Bearer <accessToken>
```

- Refresh token duoc luu bang hash trong bang `refresh_tokens`.
- Logout va refresh se revoke token cu.
- Doi mat khau thanh cong se revoke cac refresh session dang hoat dong.

Endpoint public trong cau hinh security:

| Endpoint | Muc dich |
| --- | --- |
| `POST /api/v1/auth/register` | Dang ky tai khoan khach hang. |
| `POST /api/v1/auth/login` | Dang nhap. |
| `POST /api/v1/auth/refresh` | Cap lai access token tu refresh cookie. |
| `POST /api/v1/auth/logout` | Dang xuat, xoa/revoke refresh cookie. |
| `POST /api/v1/auth/password-recovery/**` | Khoi phuc mat khau bang OTP. |
| `POST /api/v1/payment-gateways/payos/webhook` | Webhook PayOS. |
| `GET /api/v1/public/**` | API public cho homepage/customer portal. |
| `OPTIONS /**` | CORS preflight. |

Cac endpoint con lai yeu cau authenticated.

Role he thong:

| Role | Mo ta |
| --- | --- |
| `ADMIN` | Toan quyen quan tri he thong, nhan vien, chi nhanh, bao cao va cau hinh nghiep vu. |
| `MANAGER` | Quan ly van hanh theo chi nhanh, dashboard, nhan vien, ban, menu, kho, POS va bep. |
| `STAFF` | Van hanh POS, ban, order, thanh toan va mot so thao tac kho/huy hao theo luong nghiep vu. |
| `KITCHEN` | Van hanh man hinh bep, mon cho che bien, cap nhat trang thai va huy hao lien quan bep. |
| `CUSTOMER` | Su dung customer portal: profile, don hang, mon da dung, diem tich luy, coupon va review. |

## Quy Uoc API

Base URL:

```text
/api/v1
```

Response thanh cong dung wrapper `ApiResponse`:

```json
{
  "success": true,
  "message": "OK",
  "data": {},
  "timestamp": "2026-08-11T20:00:00"
}
```

Response phan trang dung `PageResponse` trong `data` hoac cau truc response rieng cua module:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Response loi duoc chuan hoa boi `GlobalExceptionHandler`:

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

Quy uoc request:

- JSON request dung `Content-Type: application/json`.
- Upload anh/import Excel dung `multipart/form-data`.
- Cac filter phan trang thuong dung `page`, `size`, `keyword`, `branchId`, `status`, `dateFrom`, `dateTo`.
- Cac API protected can `Authorization: Bearer <accessToken>`.
- Refresh token nam trong HttpOnly cookie nen client can gui cookie khi goi API cross-origin.

## Danh Sach Module Va Endpoint

### Auth

Base path: `/api/v1/auth`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `POST` | `/register` | Dang ky tai khoan khach hang. |
| `POST` | `/login` | Dang nhap, tra access token va set refresh cookie. |
| `POST` | `/refresh` | Rotate refresh token va cap access token moi. |
| `POST` | `/logout` | Dang xuat va revoke refresh token. |
| `POST` | `/change-password` | Doi mat khau cho user da dang nhap. |
| `POST` | `/admin/sync-customers` | Dong bo customer/user theo luong admin. |
| `POST` | `/password-recovery/request-otp` | Gui OTP khoi phuc mat khau. |
| `POST` | `/password-recovery/verify-otp` | Xac thuc OTP va tra reset token. |
| `POST` | `/password-recovery/reset-password` | Dat mat khau moi bang reset token. |

### Identity / Employees

Base path: `/api/v1/employees`, `/api/v1/roles`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `GET` | `/employees` | Danh sach nhan vien, ho tro filter/phan trang. |
| `GET` | `/employees/{employeeId}` | Chi tiet nhan vien. |
| `POST` | `/employees` | Tao nhan vien. |
| `PUT` | `/employees/{employeeId}` | Cap nhat nhan vien. |
| `DELETE` | `/employees/{employeeId}` | Xoa mem nhan vien. |
| `GET` | `/employees/me` | Ho so nhan vien dang dang nhap. |
| `PUT` | `/employees/me` | Cap nhat ho so ca nhan. |
| `GET` | `/roles` | Danh sach role. |

### Restaurant

Base path: `/api/v1/branches`, `/api/v1/dining-areas`, `/api/v1/tables`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `GET` | `/branches` | Danh sach chi nhanh. |
| `GET` | `/branches/{branchId}` | Chi tiet chi nhanh. |
| `POST` | `/branches` | Tao chi nhanh. |
| `PUT` | `/branches/{branchId}` | Cap nhat chi nhanh. |
| `DELETE` | `/branches/{branchId}` | Xoa chi nhanh. |
| `GET` | `/dining-areas` | Danh sach khu vuc an theo chi nhanh. |
| `POST` | `/dining-areas` | Tao khu vuc an. |
| `PUT` | `/dining-areas/{areaId}` | Cap nhat khu vuc an. |
| `DELETE` | `/dining-areas/{areaId}` | Xoa khu vuc an. |
| `GET` | `/tables` | Danh sach ban theo chi nhanh/khu vuc/trang thai. |
| `GET` | `/tables/{tableId}` | Chi tiet ban. |
| `POST` | `/tables` | Tao ban. |
| `PUT` | `/tables/{tableId}` | Cap nhat ban. |
| `DELETE` | `/tables/{tableId}` | Xoa ban. |
| `PUT` | `/tables/{tableId}/status` | Doi trang thai ban. |
| `GET` | `/tables/{tableId}/active-order` | Lay order dang mo cua ban. |
| `POST` | `/tables/{tableId}/split` | Tach order/ban. |
| `POST` | `/tables/{tableId}/unmerge` | Huy gop ban. |
| `POST` | `/tables/merge` | Gop ban. |

### Menu Va Combo

Base path: `/api/v1/categories`, `/api/v1/menu-items`, `/api/v1/combos`, `/api/v1/public`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `GET` | `/categories` | Danh sach danh muc. |
| `GET` | `/categories/{categoryId}` | Chi tiet danh muc. |
| `POST` | `/categories` | Tao danh muc. |
| `PUT` | `/categories/{categoryId}` | Cap nhat danh muc. |
| `DELETE` | `/categories/{categoryId}` | Xoa danh muc. |
| `GET` | `/menu-items` | Danh sach mon an, ho tro filter/phan trang. |
| `GET` | `/menu-items/{menuItemId}` | Chi tiet mon an. |
| `POST` | `/menu-items` | Tao mon an, ho tro upload anh multipart. |
| `PUT` | `/menu-items/{menuItemId}` | Cap nhat mon an, ho tro upload anh multipart. |
| `DELETE` | `/menu-items/{menuItemId}` | Xoa mon an. |
| `GET` | `/combos` | Danh sach combo. |
| `GET` | `/combos/{id}` | Chi tiet combo. |
| `POST` | `/combos` | Tao combo multipart. |
| `PUT` | `/combos/{id}` | Cap nhat combo multipart. |
| `DELETE` | `/combos/{id}` | Xoa combo. |
| `GET` | `/public/menu/popular` | Danh sach mon pho bien cho homepage. |
| `GET` | `/public/menu-items/{menuItemId}/reviews` | Review public cua mon. |

### Customers, Loyalty Va Portal

Base path: `/api/v1/customers`, `/api/v1/customer-tiers`, `/api/v1/me`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `GET` | `/customers/lookup` | Tim nhanh khach hang theo keyword. |
| `GET` | `/customers` | Danh sach khach hang. |
| `GET` | `/customers/{customerId}` | Chi tiet khach hang. |
| `POST` | `/customers` | Tao khach hang. |
| `POST` | `/customers/quick-create` | Tao nhanh khach hang trong POS. |
| `PUT` | `/customers/{customerId}` | Cap nhat khach hang. |
| `DELETE` | `/customers/{customerId}` | Xoa mem khach hang. |
| `GET` | `/customers/{customerId}/loyalty-transactions` | Lich su diem cua khach hang. |
| `GET` | `/customer-tiers` | Danh sach hang thanh vien. |
| `GET` | `/customer-tiers/{tierId}` | Chi tiet hang thanh vien. |
| `POST` | `/customer-tiers` | Tao hang thanh vien. |
| `PUT` | `/customer-tiers/{tierId}` | Cap nhat hang thanh vien. |
| `DELETE` | `/customer-tiers/{tierId}` | Xoa hang thanh vien. |
| `GET` | `/me/profile` | Customer xem ho so. |
| `PUT` | `/me/profile` | Customer cap nhat ho so. |
| `GET` | `/me/loyalty/transactions` | Customer xem lich su diem. |
| `GET` | `/me/orders` | Customer xem lich su don. |
| `GET` | `/me/dishes-eaten` | Customer xem mon da dung. |
| `POST` | `/me/reviews` | Customer tao review. |
| `GET` | `/me/reviews` | Customer xem review cua minh. |
| `GET` | `/me/coupons` | Customer xem coupon. |

### Inventory

Base path: `/api/v1/inventory`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `GET` | `/units` | Danh sach don vi do. |
| `GET` | `/` | Danh sach hang ton kho. |
| `GET` | `/summary` | Tong quan ton kho. |
| `GET` | `/reports/movements` | Bao cao bien dong kho. |
| `GET` | `/alerts` | Canh bao ton kho thap/het. |
| `GET` | `/import/template` | Tai file template import Excel. |
| `POST` | `/import/preview` | Preview file Excel truoc khi import. |
| `POST` | `/import/commit` | Commit import Excel. |
| `GET` | `/{inventoryId}` | Chi tiet item kho. |
| `GET` | `/{inventoryId}/history` | Lich su dieu chinh item. |
| `POST` | `/` | Tao item kho. |
| `POST` | `/{inventoryId}/restock` | Nhap them ton kho. |
| `PUT` | `/{inventoryId}` | Cap nhat item kho. |
| `DELETE` | `/{inventoryId}` | Xoa item kho. |

### Order, Kitchen, Billing Va Payment

Base path: `/api/v1/orders`, `/api/v1/kitchen`, `/api/v1/bills`, `/api/v1/payment-gateways`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `POST` | `/orders` | Tao order cho ban/POS. |
| `GET` | `/orders/{orderId}` | Chi tiet order. |
| `GET` | `/orders/{orderId}/bills` | Danh sach bill cua order. |
| `PUT` | `/orders/{orderId}/customer` | Gan khach hang vao order. |
| `PUT` | `/orders/order-items/{orderItemId}/serve` | Danh dau mon da phuc vu. |
| `GET` | `/kitchen/orders/pending` | Danh sach mon dang cho bep. |
| `PUT` | `/kitchen/order-items/{orderItemId}/status` | Cap nhat trang thai mon bep. |
| `POST` | `/kitchen/order-items/{orderItemId}/complete` | Hoan thanh mon bep. |
| `GET` | `/bills/preview` | Preview bill truoc khi thanh toan. |
| `GET` | `/bills/history` | Lich su bill. |
| `GET` | `/bills/{billId}` | Chi tiet bill. |
| `GET` | `/bills/{billId}/invoice.pdf` | Tai hoa don PDF. |
| `POST` | `/bills` | Tao bill. |
| `POST` | `/bills/{billId}/payments` | Ghi nhan thanh toan. |
| `POST` | `/bills/{billId}/payos/qr` | Tao QR PayOS. |
| `GET` | `/bills/{billId}/payos/qr` | Lay QR PayOS moi nhat cua bill. |
| `POST` | `/bills/{billId}/payos/qr/cancel` | Huy QR PayOS dang cho. |
| `GET` | `/payment-gateways/transactions/{transactionId}` | Lay trang thai giao dich payment gateway. |
| `POST` | `/payment-gateways/payos/webhook` | Webhook PayOS. |

### Report

Base path: `/api/v1/reports`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `GET` | `/dashboard` | Du lieu dashboard tong hop. |
| `GET` | `/revenue/summary` | Tong hop doanh thu theo khoang thoi gian. |
| `GET` | `/revenue/timeseries` | Chuoi doanh thu theo ngay/thang/nam. |
| `GET` | `/payments/method-breakdown` | Co cau phuong thuc thanh toan. |
| `GET` | `/bills/status-summary` | Tong hop trang thai bill. |

### Waste Requests

Base path: `/api/v1/waste-requests`

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| `POST` | `/` | Tao yeu cau huy hao multipart. |
| `GET` | `/` | Danh sach yeu cau huy hao. |
| `GET` | `/stats` | Thong ke huy hao. |
| `GET` | `/export` | Xuat bao cao huy hao. |
| `GET` | `/{id}` | Chi tiet yeu cau. |
| `PUT` | `/{id}/approve` | Duyet yeu cau. |
| `PUT` | `/{id}/reject` | Tu choi yeu cau. |
| `GET` | `/pending-count` | So yeu cau dang cho duyet. |

## Tich Hop Ben Ngoai

### PayOS

PayOS duoc dung cho QR payment trong POS.

Luong chinh:

1. FE tao bill hoac lay bill dang mo.
2. FE goi `POST /api/v1/bills/{billId}/payos/qr`.
3. Backend tao payment link/QR tren PayOS va luu `PaymentGatewayTransaction`.
4. FE hien QR, checkout URL va polling trang thai transaction.
5. PayOS goi webhook ve `/api/v1/payment-gateways/payos/webhook`.
6. Backend verify checksum, dong bo trang thai bill/payment.
7. FE co the goi lai transaction de refresh trang thai.

Khi test webhook local, can public URL nhu ngrok:

```powershell
.\scripts\start-payos-ngrok.ps1
```

Cap nhat `PAYOS_WEBHOOK_URL` bang public URL do ngrok cung cap.

### Cloudinary

Cloudinary duoc dung cho anh mon an/combo.

Neu test local khong can upload cloud:

```properties
app.cloudinary.enabled=false
```

Neu bat:

```properties
app.cloudinary.enabled=true
app.cloudinary.cloud-name=...
app.cloudinary.api-key=...
app.cloudinary.api-secret=...
app.cloudinary.folder=goldenheart/menu-items
```

### Email OTP

Password recovery dung Spring Mail. Voi Gmail can App Password, khong dung mat khau dang nhap Gmail thong thuong.

De test local khong gui email that, co the bat log delivery neu service ho tro cau hinh nay:

```properties
app.password-recovery.dev-log-delivery=true
```

## Tai Lieu Va Cong Cu Kem Theo

Tai lieu co san trong backend:

| File/Thu muc | Noi dung |
| --- | --- |
| `AUTH_SETUP.md` | Huong dan rieng ve JWT auth, refresh token va change password. |
| `API_TESTING_RUNBOOK.md` | Runbook test API bang Postman/Newman. |
| `PROJECT_STRUCTURE.md` | Ghi chu cau truc module backend. |
| `BE_ROADMAP.md` | Roadmap/cong viec backend. |
| `postman/` | Postman collection va environment E2E. |
| `sql/` | Script reset va seed database local. |
| `ngrok/` | Cau hinh lien quan webhook PayOS/ngrok. |
| `scripts/` | Script tien ich khi test PayOS/local. |

## Quy Uoc Phat Trien

- Controller chi dieu phoi request/response, khong nhoi nghiep vu lon.
- Service xu ly logic nghiep vu, phan quyen, transaction va validate cheo.
- Repository chi truy van database, uu tien method ro nghia hoac query co ten projection.
- Khong tra entity truc tiep cho FE neu API da co response DTO.
- Request DTO phai dung validation annotation phu hop.
- Response DTO chi tra truong an toan, khong tra password hash, token hash, secret hoac thong tin noi bo.
- Moi module nen giu boundary rieng: `controller`, `service`, `repository`, `entity`, `dto`.
- Cac xoa nghiep vu dang dung soft delete voi `deleted_at` o nhieu entity, can can nhac khi query duplicate va lookup.
- Cac thao tac lien quan chi nhanh can truyen/kiem tra `branchId` de dung scope van hanh.
- Khong commit `application-local.properties`, build output, log runtime hoac secret.

## Troubleshooting

### Backend khong ket noi duoc database

Kiem tra:

- Database da chay chua.
- URL JDBC dung port/schema chua.
- Username/password trong `application-local.properties`.
- MySQL co cho phep public key retrieval/SSL theo cau hinh dang dung khong.

### FE goi API bi CORS hoac mat cookie

Kiem tra:

- Backend dang chay port `1010`.
- FE dung `VITE_API_BASE_URL=http://localhost:1010/api/v1` hoac dung proxy `/api/v1`.
- Axios `withCredentials=true` da duoc bat trong FE.
- Cookie refresh token path la `/api/v1/auth`.
- Neu deploy HTTPS, can bat cookie secure/same-site phu hop.

### Login thanh cong nhung API protected bi 401

Kiem tra:

- FE da luu `accessToken` vao localStorage chua.
- Request co header `Authorization: Bearer <token>` khong.
- `JWT_SECRET` luc login va luc verify co cung gia tri khong.
- Token da het han chua.

### Khong tao duoc PDF hoa don tieng Viet

Kiem tra:

- Template `src/main/resources/templates/billing/bill-invoice.html`.
- Font Unicode tren may chay server.
- Thu `INVOICE_PDF_FONT_PATH` tro den font ho tro tieng Viet.

### PayOS webhook khong cap nhat giao dich

Kiem tra:

- `PAYOS_WEBHOOK_URL` phai la public URL, khong phai localhost.
- Checksum key dung voi merchant PayOS.
- Backend public endpoint `/api/v1/payment-gateways/payos/webhook` khong bi chan.
- Ngrok tunnel con song.

### Upload anh that bai

Kiem tra:

- `app.cloudinary.enabled`.
- Cloud name, API key, API secret.
- Gioi han file `spring.servlet.multipart.max-file-size` va `app.cloudinary.max-file-size`.
- Request FE co dung `multipart/form-data`.
