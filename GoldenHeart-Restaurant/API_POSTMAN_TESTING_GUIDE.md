# GoldenHeart Restaurant API Testing Guide

This guide lists the APIs that currently exist in the project, shows how to call them, and gives a practical Postman test order for a fresh local database.

## 1. Current API Scope

The project currently exposes these API groups:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/roles`
- `GET /api/v1/employees`
- `GET /api/v1/employees/{employeeId}`
- `POST /api/v1/employees`
- `PUT /api/v1/employees/{employeeId}`
- `DELETE /api/v1/employees/{employeeId}`
- `GET /api/v1/employees/me`
- `PUT /api/v1/employees/me`
- `GET /api/v1/customers`
- `GET /api/v1/customers/{customerId}`
- `POST /api/v1/customers`
- `PUT /api/v1/customers/{customerId}`
- `DELETE /api/v1/customers/{customerId}`

## 2. Recommended Test Order For Empty Database

Because your database is empty, use this order:

1. Run [01_reset_local_database.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/01_reset_local_database.sql) in MySQL Workbench.
2. Start the Spring Boot app once.
   The app will auto-create tables because `spring.jpa.hibernate.ddl-auto=update`.
3. Let the app bootstrap roles and the local admin account from `application.properties`.
4. Optionally run [02_seed_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/02_seed_reference_data.sql) to add sample restaurant and branch records.
5. Import the Postman files:
   - [GoldenHeart-Restaurant.postman_collection.json](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/postman/GoldenHeart-Restaurant.postman_collection.json)
   - [GoldenHeart-Restaurant.local.postman_environment.json](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/postman/GoldenHeart-Restaurant.local.postman_environment.json)
6. Run requests in this order:
   - `Auth / Login Admin`
   - `Roles / Get Roles`
   - `Employees / Create Manager By Admin`
   - `Auth / Login Manager`
   - `Employees / Manager Create Staff Without RoleId`
   - `Auth / Login Staff`
   - `Employees / Staff Get My Profile`
   - `Customers / Create Customer 01`
   - `Customers / Get Customers`
   - then run the negative cases

## 3. Authentication Notes

- Base URL local: `http://localhost:1010`
- Access token goes in header:

```http
Authorization: Bearer <access-token>
```

- Refresh token is returned as an `HttpOnly` cookie on login.
- In Postman, keep cookie jar enabled. `refresh` and `logout` use the cookie automatically if you stay on the same host.

## 4. Default Bootstrap Account

When the app starts on a clean database, it auto-seeds:

- username: `admin`
- password: `Admin123`

## 5. Response Shape

### Success response

```json
{
  "success": true,
  "message": "Customers retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  },
  "timestamp": "2026-04-10T00:10:00Z"
}
```

### Business error response

```json
{
  "success": false,
  "message": "Email already exists",
  "errors": null,
  "timestamp": "2026-04-10T00:10:00Z"
}
```

### Validation error response

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "password": "Password must contain at least 1 uppercase letter, 1 lowercase letter, and 1 number"
  },
  "timestamp": "2026-04-10T00:10:00Z"
}
```

## 6. Endpoint Summary With Examples

### 6.1 Auth

#### `POST /api/v1/auth/register`

Use this for customer self-registration.

Request:

```json
{
  "username": "customer01",
  "email": "customer01@example.com",
  "password": "Customer123",
  "fullName": "Customer One"
}
```

Success response:

```json
{
  "success": true,
  "message": "Register successfully",
  "data": {
    "userId": 2,
    "username": "customer01",
    "email": "customer01@example.com",
    "fullName": "Customer One",
    "role": "CUSTOMER",
    "createdAt": "2026-04-10T00:10:00"
  },
  "timestamp": "2026-04-10T00:10:00Z"
}
```

#### `POST /api/v1/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "Admin123"
}
```

Success response:

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "accessToken": "<jwt-access-token>",
    "tokenType": "Bearer",
    "expiresAt": "2026-04-10T00:25:00Z",
    "username": "admin",
    "role": "ADMIN"
  },
  "timestamp": "2026-04-10T00:10:00Z"
}
```

#### `POST /api/v1/auth/refresh`

No JSON body.
Needs the refresh cookie from login.

#### `POST /api/v1/auth/logout`

No JSON body.
Needs the refresh cookie from login.

### 6.2 Roles

#### `GET /api/v1/roles`

Auth:
- `ADMIN`
- `MANAGER`

Example response:

```json
{
  "success": true,
  "message": "Roles retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "ADMIN",
      "description": "ADMIN role"
    },
    {
      "id": 5,
      "name": "CUSTOMER",
      "description": "CUSTOMER role"
    },
    {
      "id": 4,
      "name": "KITCHEN",
      "description": "KITCHEN role"
    },
    {
      "id": 2,
      "name": "MANAGER",
      "description": "MANAGER role"
    },
    {
      "id": 3,
      "name": "STAFF",
      "description": "STAFF role"
    }
  ],
  "timestamp": "2026-04-10T00:10:00Z"
}
```

### 6.3 Employees

#### `GET /api/v1/employees`

Auth:
- `ADMIN`
- `MANAGER`

Example:

```http
GET /api/v1/employees?page=0&size=10&keyword=staff
Authorization: Bearer <admin-token>
```

#### `POST /api/v1/employees`

Auth:
- `ADMIN`
- `MANAGER`

Admin example:

```json
{
  "username": "manager01",
  "password": "Manager123",
  "roleId": 2,
  "fullName": "Manager One",
  "employeeCode": "MGR001",
  "email": "manager01@goldenheart.com",
  "phone": "0900000002",
  "branchId": null,
  "dateOfBirth": "1995-05-10",
  "gender": "Male",
  "hireDate": "2026-04-10",
  "salary": 15000000,
  "address": "District 1",
  "internalNotes": "Created by admin"
}
```

Important:
- If actor is `ADMIN`, `roleId` is required.
- If actor is `MANAGER`, do not send `roleId`.
- Manager-created employee will default to `STAFF`.

#### `PUT /api/v1/employees/{employeeId}`

Auth:
- `ADMIN`
- `MANAGER`

Example:

```json
{
  "fullName": "Staff One Updated",
  "phone": "0900000099",
  "address": "District 3",
  "salary": 8000000,
  "status": "ACTIVE"
}
```

#### `DELETE /api/v1/employees/{employeeId}`

Auth:
- `ADMIN`

#### `GET /api/v1/employees/me`

Auth:
- any authenticated user

#### `PUT /api/v1/employees/me`

Auth:
- any authenticated user

Example:

```json
{
  "fullName": "Staff Self Updated",
  "email": "staff01.updated@goldenheart.com",
  "phone": "0900000010",
  "address": "Thu Duc",
  "dateOfBirth": "2001-01-10",
  "gender": "Male"
}
```

### 6.4 Customers

#### `POST /api/v1/customers`

Auth:
- `ADMIN`
- `MANAGER`

Request:

```json
{
  "name": "Nguyen Van A",
  "email": "customerA@example.com",
  "phone": "0911111111",
  "customerCode": "CUS001",
  "address": "HCM City",
  "dateOfBirth": "2000-02-20",
  "gender": "Male",
  "note": "VIP"
}
```

Sample response:

```json
{
  "success": true,
  "message": "Customer created successfully",
  "data": {
    "id": 1,
    "customerCode": "CUS001",
    "name": "Nguyen Van A",
    "phone": "0911111111",
    "email": "customerA@example.com",
    "loyaltyPoints": 0,
    "address": "HCM City",
    "dateOfBirth": "2000-02-20",
    "gender": "Male",
    "note": "VIP",
    "lastVisitAt": null,
    "createdAt": "2026-04-10T00:30:00",
    "updatedAt": "2026-04-10T00:30:00"
  },
  "timestamp": "2026-04-10T00:30:00Z"
}
```

#### `PUT /api/v1/customers/{customerId}`

Auth:
- `ADMIN`
- `MANAGER`

#### `DELETE /api/v1/customers/{customerId}`

Auth:
- `ADMIN`

## 7. Best Postman Test Cases

### Happy path

1. Login admin.
2. Get roles and store `managerRoleId` and `staffRoleId`.
3. Create manager by admin.
4. Login manager.
5. Manager creates staff without roleId.
6. Login staff.
7. Staff gets own profile.
8. Staff updates own profile.
9. Admin creates customers.
10. Manager lists customers and employees.

### Negative path

1. Login with wrong password -> `401`
2. Register duplicate email -> `409`
3. Create employee with duplicate username -> `409`
4. Manager create employee with `roleId` -> `403`
5. Manager update employee role -> `403`
6. Staff get employee list -> `403`
7. Admin delete self -> `403`
8. Get missing customer -> `404`
9. Refresh without cookie -> `401`

## 8. Optional Reference Data

If you want branch data available for future employee tests, run [02_seed_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/02_seed_reference_data.sql).
