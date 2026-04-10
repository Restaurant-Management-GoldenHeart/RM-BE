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
- `GET /api/v1/menu-items`
- `GET /api/v1/menu-items/{menuItemId}`
- `POST /api/v1/menu-items`
- `PUT /api/v1/menu-items/{menuItemId}`
- `DELETE /api/v1/menu-items/{menuItemId}`
- `POST /api/v1/kitchen/order-items/{orderItemId}/complete`

## 2. Recommended Test Order For Empty Database

Because your database is empty, use this order:

1. Run [01_reset_local_database.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/01_reset_local_database.sql) in MySQL Workbench.
2. Start the Spring Boot app once.
   The app will auto-create tables because `spring.jpa.hibernate.ddl-auto=update`.
3. Let the app bootstrap roles and the local admin account from `application.properties`.
4. Optionally run [02_seed_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/02_seed_reference_data.sql) to add sample restaurant and branch records.
5. If you want to test `menu-items`, run [05_seed_menu_inventory_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/05_seed_menu_inventory_reference_data.sql) to seed categories, ingredients and inventory.
6. Import the Postman files:
   - [GoldenHeart-Restaurant.postman_collection.json](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/postman/GoldenHeart-Restaurant.postman_collection.json)
   - [GoldenHeart-Restaurant.local.postman_environment.json](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/postman/GoldenHeart-Restaurant.local.postman_environment.json)
7. Run requests in this order:
   - `Auth / Login Admin`
   - `Roles / Get Roles`
   - `Employees / Create Manager By Admin`
   - `Auth / Login Manager`
   - `Employees / Manager Create Staff Without RoleId`
   - `Auth / Login Staff`
   - `Menu / Create Menu Item Pho Bo Tai`
   - `Menu / Get Menu Items`
   - `Menu / Get Menu Item By Id`
   - `Menu / Update Menu Item`
   - `Employees / Staff Get My Profile`
   - `Customers / Create Customer 01`
   - `Customers / Get Customers`
   - then run [06_seed_order_item_for_kitchen_test.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/06_seed_order_item_for_kitchen_test.sql)
   - set `orderItemId` in Postman environment from SQL result
   - `Kitchen / Complete Order Item As Admin`
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

Success response:

```json
{
  "success": true,
  "message": "Refresh token successfully",
  "data": {
    "accessToken": "<new-jwt-access-token>",
    "tokenType": "Bearer",
    "expiresAt": "2026-04-10T00:40:00Z",
    "username": "admin",
    "role": "ADMIN"
  },
  "timestamp": "2026-04-10T00:25:10Z"
}
```

#### `POST /api/v1/auth/logout`

No JSON body.
Needs the refresh cookie from login.

Success response:

```json
{
  "success": true,
  "message": "Logout successfully",
  "data": null,
  "timestamp": "2026-04-10T00:30:00Z"
}
```

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

Sample response:

```json
{
  "success": true,
  "message": "Employees retrieved successfully",
  "data": {
    "content": [
      {
        "id": 3,
        "username": "staff01",
        "status": "ACTIVE",
        "roleId": 3,
        "roleName": "STAFF",
        "fullName": "Staff One",
        "employeeCode": "EMP001",
        "email": "staff01@goldenheart.com",
        "phone": "0900000001",
        "branchId": null,
        "branchName": null,
        "dateOfBirth": "2001-01-10",
        "gender": "Male",
        "hireDate": "2026-04-10",
        "salary": 7000000,
        "address": "HCM City",
        "internalNotes": "Night shift",
        "createdAt": "2026-04-10T00:15:00",
        "updatedAt": "2026-04-10T00:15:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-04-10T00:16:00Z"
}
```

#### `GET /api/v1/employees/{employeeId}`

Auth:
- `ADMIN`
- `MANAGER`

Sample response:

```json
{
  "success": true,
  "message": "Employee retrieved successfully",
  "data": {
    "id": 3,
    "username": "staff01",
    "status": "ACTIVE",
    "roleId": 3,
    "roleName": "STAFF",
    "fullName": "Staff One",
    "employeeCode": "EMP001",
    "email": "staff01@goldenheart.com",
    "phone": "0900000001",
    "branchId": null,
    "branchName": null,
    "dateOfBirth": "2001-01-10",
    "gender": "Male",
    "hireDate": "2026-04-10",
    "salary": 7000000,
    "address": "HCM City",
    "internalNotes": "Night shift",
    "createdAt": "2026-04-10T00:15:00",
    "updatedAt": "2026-04-10T00:15:00"
  },
  "timestamp": "2026-04-10T00:16:30Z"
}
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

Success response:

```json
{
  "success": true,
  "message": "Employee deleted successfully",
  "data": null,
  "timestamp": "2026-04-10T00:22:00Z"
}
```

#### `GET /api/v1/employees/me`

Auth:
- any authenticated user

Sample response:

```json
{
  "success": true,
  "message": "My profile retrieved successfully",
  "data": {
    "id": 3,
    "username": "staff01",
    "status": "ACTIVE",
    "roleName": "STAFF",
    "fullName": "Staff One",
    "employeeCode": "EMP001",
    "email": "staff01@goldenheart.com",
    "phone": "0900000001",
    "branchId": null,
    "branchName": null,
    "dateOfBirth": "2001-01-10",
    "gender": "Male",
    "hireDate": "2026-04-10",
    "address": "HCM City",
    "createdAt": "2026-04-10T00:15:00",
    "updatedAt": "2026-04-10T00:15:00"
  },
  "timestamp": "2026-04-10T00:20:00Z"
}
```

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

#### `GET /api/v1/customers`

Auth:
- `ADMIN`
- `MANAGER`

Example:

```http
GET /api/v1/customers?page=0&size=10&keyword=nguyen
Authorization: Bearer <manager-token>
```

Sample response:

```json
{
  "success": true,
  "message": "Customers retrieved successfully",
  "data": {
    "content": [
      {
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
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-04-10T00:31:00Z"
}
```

### 6.5 Menu Items

#### `GET /api/v1/menu-items`

Auth:
- `ADMIN`
- `MANAGER`
- `STAFF`
- `KITCHEN`

Query params:
- `keyword` optional
- `branchId` optional
- `categoryId` optional
- `page` default `0`
- `size` default `10`

Example:

```http
GET /api/v1/menu-items?page=0&size=10&branchId=1&categoryId=1
Authorization: Bearer <staff-token>
```

Sample response:

```json
{
  "success": true,
  "message": "Menu items retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "branchId": 1,
        "branchName": "Golden Heart Branch 1",
        "categoryId": 1,
        "categoryName": "Pho",
        "name": "Pho Bo Tai",
        "description": "Pho bo tai tai branch 1",
        "price": 65000,
        "status": "AVAILABLE",
        "recipes": [
          {
            "ingredientId": 1,
            "ingredientName": "Beef",
            "unit": "gram",
            "quantity": 120
          },
          {
            "ingredientId": 2,
            "ingredientName": "Rice Noodle",
            "unit": "gram",
            "quantity": 180
          }
        ]
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-04-10T00:40:00Z"
}
```

#### `GET /api/v1/menu-items/{menuItemId}`

Auth:
- `ADMIN`
- `MANAGER`
- `STAFF`
- `KITCHEN`

#### `POST /api/v1/menu-items`

Auth:
- `ADMIN`

Request:

```json
{
  "branchId": 1,
  "categoryId": 1,
  "name": "Pho Bo Tai",
  "description": "Pho bo tai tai branch 1",
  "price": 65000,
  "status": "AVAILABLE",
  "recipes": [
    {
      "ingredientId": 1,
      "quantity": 120
    },
    {
      "ingredientId": 2,
      "quantity": 180
    },
    {
      "ingredientId": 3,
      "quantity": 500
    },
    {
      "ingredientId": 4,
      "quantity": 20
    }
  ]
}
```

Success response:

```json
{
  "success": true,
  "message": "Menu item created successfully",
  "data": {
    "id": 1,
    "branchId": 1,
    "branchName": "Golden Heart Branch 1",
    "categoryId": 1,
    "categoryName": "Pho",
    "name": "Pho Bo Tai",
    "description": "Pho bo tai tai branch 1",
    "price": 65000,
    "status": "AVAILABLE",
    "recipes": [
      {
        "ingredientId": 1,
        "ingredientName": "Beef",
        "unit": "gram",
        "quantity": 120
      },
      {
        "ingredientId": 2,
        "ingredientName": "Rice Noodle",
        "unit": "gram",
        "quantity": 180
      },
      {
        "ingredientId": 3,
        "ingredientName": "Broth",
        "unit": "ml",
        "quantity": 500
      },
      {
        "ingredientId": 4,
        "ingredientName": "Onion",
        "unit": "gram",
        "quantity": 20
      }
    ]
  },
  "timestamp": "2026-04-10T00:39:00Z"
}
```

#### `PUT /api/v1/menu-items/{menuItemId}`

Auth:
- `ADMIN`

Request:

```json
{
  "branchId": 1,
  "categoryId": 1,
  "name": "Pho Bo Tai Special",
  "description": "Updated pho item",
  "price": 70000,
  "status": "AVAILABLE",
  "recipes": [
    {
      "ingredientId": 1,
      "quantity": 140
    },
    {
      "ingredientId": 2,
      "quantity": 180
    },
    {
      "ingredientId": 3,
      "quantity": 550
    },
    {
      "ingredientId": 4,
      "quantity": 25
    }
  ]
}
```

#### `DELETE /api/v1/menu-items/{menuItemId}`

Auth:
- `ADMIN`

### 6.6 Kitchen

#### `POST /api/v1/kitchen/order-items/{orderItemId}/complete`

Auth:
- `ADMIN`
- `KITCHEN`

No request body.

This endpoint:
- loads `order_item`
- loads its `menu_item` recipe
- locks inventory by branch and ingredient
- deducts stock
- creates `stock_movements`
- updates `order_item.status` to `COMPLETED`
- updates parent `order.status` to `PROCESSING` or `COMPLETED`

Sample response:

```json
{
  "success": true,
  "message": "Order item completed and stock deducted successfully",
  "data": {
    "orderItemId": 1,
    "orderId": 1,
    "menuItemName": "Pho Bo Tai",
    "status": "COMPLETED",
    "deductions": [
      {
        "ingredientId": 1,
        "ingredientName": "Beef",
        "unit": "gram",
        "deductedQuantity": 240,
        "remainingQuantity": 4760
      },
      {
        "ingredientId": 2,
        "ingredientName": "Rice Noodle",
        "unit": "gram",
        "deductedQuantity": 360,
        "remainingQuantity": 7640
      }
    ]
  },
  "timestamp": "2026-04-10T00:50:00Z"
}
```

#### `GET /api/v1/customers/{customerId}`

Auth:
- `ADMIN`
- `MANAGER`

Sample response:

```json
{
  "success": true,
  "message": "Customer retrieved successfully",
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
  "timestamp": "2026-04-10T00:31:10Z"
}
```

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

Example:

```json
{
  "name": "Nguyen Van A Updated",
  "phone": "0922222222",
  "address": "District 7",
  "note": "Returned customer"
}
```

#### `DELETE /api/v1/customers/{customerId}`

Auth:
- `ADMIN`

Success response:

```json
{
  "success": true,
  "message": "Customer deleted successfully",
  "data": null,
  "timestamp": "2026-04-10T00:35:00Z"
}
```

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

If you want 3 sample customers directly from MySQL Workbench, run [04_optional_seed_customers.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/04_optional_seed_customers.sql).

If you want to test `menu-items`, run [05_seed_menu_inventory_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/05_seed_menu_inventory_reference_data.sql).

If you want to test the kitchen complete API after creating a menu item named `Pho Bo Tai`, run [06_seed_order_item_for_kitchen_test.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/06_seed_order_item_for_kitchen_test.sql).

## 9. Full API Matrix

| Method | Path | Auth | Main purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Public | Customer self-register |
| `POST` | `/api/v1/auth/login` | Public | Login and receive access token + refresh cookie |
| `POST` | `/api/v1/auth/refresh` | Refresh cookie | Rotate tokens |
| `POST` | `/api/v1/auth/logout` | Refresh cookie | Revoke refresh token and clear cookie |
| `GET` | `/api/v1/roles` | `ADMIN`, `MANAGER` | Get role list |
| `GET` | `/api/v1/employees` | `ADMIN`, `MANAGER` | Paginated employee list |
| `GET` | `/api/v1/employees/{employeeId}` | `ADMIN`, `MANAGER` | Employee detail |
| `POST` | `/api/v1/employees` | `ADMIN`, `MANAGER` | Create employee |
| `PUT` | `/api/v1/employees/{employeeId}` | `ADMIN`, `MANAGER` | Update employee |
| `DELETE` | `/api/v1/employees/{employeeId}` | `ADMIN` | Soft-delete employee |
| `GET` | `/api/v1/employees/me` | Any authenticated user | View current profile |
| `PUT` | `/api/v1/employees/me` | Any authenticated user | Update current profile |
| `GET` | `/api/v1/customers` | `ADMIN`, `MANAGER` | Paginated customer list |
| `GET` | `/api/v1/customers/{customerId}` | `ADMIN`, `MANAGER` | Customer detail |
| `POST` | `/api/v1/customers` | `ADMIN`, `MANAGER` | Create customer |
| `PUT` | `/api/v1/customers/{customerId}` | `ADMIN`, `MANAGER` | Update customer |
| `DELETE` | `/api/v1/customers/{customerId}` | `ADMIN` | Soft-delete customer |
| `GET` | `/api/v1/menu-items` | `ADMIN`, `MANAGER`, `STAFF`, `KITCHEN` | List menu items |
| `GET` | `/api/v1/menu-items/{menuItemId}` | `ADMIN`, `MANAGER`, `STAFF`, `KITCHEN` | Menu item detail |
| `POST` | `/api/v1/menu-items` | `ADMIN` | Create menu item with recipe |
| `PUT` | `/api/v1/menu-items/{menuItemId}` | `ADMIN` | Update menu item with recipe |
| `DELETE` | `/api/v1/menu-items/{menuItemId}` | `ADMIN` | Delete menu item |
| `POST` | `/api/v1/kitchen/order-items/{orderItemId}/complete` | `ADMIN`, `KITCHEN` | Complete kitchen production and deduct stock |

## 10. Postman Test Flow From Empty Database

### Phase A. Prepare database

1. Open MySQL Workbench.
2. Run [01_reset_local_database.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/01_reset_local_database.sql).
3. Start the backend.
4. Wait until app starts successfully.
5. Optional: run [02_seed_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/02_seed_reference_data.sql).
6. Optional: run [04_optional_seed_customers.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/04_optional_seed_customers.sql) if you want immediate customer data before testing list/detail APIs.
7. Run [05_seed_menu_inventory_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/05_seed_menu_inventory_reference_data.sql) before menu tests.

Expected result:

- tables are auto-created
- roles are bootstrapped
- admin account is created
- branch `1` exists after reference seed
- category `1` and ingredient ids `1..4` exist after menu inventory seed

### Phase B. Import Postman artifacts

1. Import [GoldenHeart-Restaurant.postman_collection.json](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/postman/GoldenHeart-Restaurant.postman_collection.json)
2. Import [GoldenHeart-Restaurant.local.postman_environment.json](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/postman/GoldenHeart-Restaurant.local.postman_environment.json)
3. Select environment `GoldenHeart Restaurant Local`

### Phase C. Detailed test cases in execution order

#### C1. Login admin

- Request: `Auth / Login Admin`
- Expected status: `200`
- Why first: this gives you `adminAccessToken`

Success example:

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresAt": "2026-04-10T00:25:00Z",
    "username": "admin",
    "role": "ADMIN"
  },
  "timestamp": "2026-04-10T00:10:00Z"
}
```

#### C2. Get roles

- Request: `Roles / Get Roles`
- Expected status: `200`
- After this request, Postman will store:
  - `adminRoleId`
  - `managerRoleId`
  - `staffRoleId`
  - `kitchenRoleId`
  - `customerRoleId`

#### C3. Create manager by admin

- Request: `Employees / Create Manager By Admin`
- Expected status: `201`
- Postman stores `managerId`

#### C4. Login manager

- Request: `Auth / Login Manager`
- Expected status: `200`
- Postman stores `managerAccessToken`

#### C5. Create staff by admin

- Request: `Employees / Create Staff By Admin`
- Expected status: `201`
- Postman stores `staffId`

#### C6. Login staff

- Request: `Auth / Login Staff`
- Expected status: `200`
- Postman stores `staffAccessToken`

#### C7. Staff view own profile

- Request: `Employees / Staff Get My Profile`
- Expected status: `200`

#### C8. Staff update own profile

- Request: `Employees / Staff Update My Profile`
- Expected status: `200`

#### C9. Manager creates a staff account without roleId

- Request: `Employees / Manager Create Staff Without RoleId`
- Expected status: `201`
- Meaning: manager can create employee, but role is auto-defaulted to `STAFF`

#### C10. Manager tries to create employee with roleId

- Request: `Employees / Manager Create With RoleId Forbidden`
- Expected status: `403`

Error example:

```json
{
  "success": false,
  "message": "Manager cannot assign role when creating employee",
  "errors": null,
  "timestamp": "2026-04-10T00:15:00Z"
}
```

#### C11. Manager updates employee basic info

- Request: `Employees / Manager Update Employee Basic Info`
- Expected status: `200`

#### C12. Manager tries to update role

- Request: `Employees / Manager Update Role Forbidden`
- Expected status: `403`

#### C13. Staff tries to list employees

- Request: `Employees / Staff Get Employees Forbidden`
- Expected status: `403`

#### C14. Create customer 01 by admin

- Request: `Customers / Create Customer 01`
- Expected status: `201`
- Postman stores `customerId`

#### C15. Create customer 02 by manager

- Request: `Customers / Create Customer 02`
- Expected status: `201`

#### C16. Create duplicate customer email

- Request: `Customers / Create Customer Duplicate Email`
- Expected status: `409`

Conflict example:

```json
{
  "success": false,
  "message": "Email already exists",
  "errors": null,
  "timestamp": "2026-04-10T00:20:00Z"
}
```

#### C17. Get customers list

- Request: `Customers / Get Customers`
- Expected status: `200`

#### C18. Get customer by id

- Request: `Customers / Get Customer By Id`
- Expected status: `200`

#### C19. Update customer

- Request: `Customers / Update Customer`
- Expected status: `200`

#### C20. Delete customer

- Request: `Customers / Delete Customer`
- Expected status: `200`

#### C21. Get deleted customer

- Request: `Customers / Get Deleted Customer 404`
- Expected status: `404`

Not found example:

```json
{
  "success": false,
  "message": "Customer not found",
  "errors": null,
  "timestamp": "2026-04-10T00:25:00Z"
}
```

#### C22. Refresh token

- Request: `Auth / Refresh Current Session`
- Expected status: `200`
- Important: run this in the same Postman session after a successful login so the refresh cookie still exists

#### C23. Logout

- Request: `Auth / Logout Current Session`
- Expected status: `200`

#### C24. Create menu item

- Request: `Menu / Create Menu Item Pho Bo Tai`
- Expected status: `201`
- Requires `branchId=1`, `categoryId=1`, `ingredientIds=1..4`
- Postman stores `menuItemId`

#### C25. Get menu items

- Request: `Menu / Get Menu Items`
- Expected status: `200`

#### C26. Get menu item by id

- Request: `Menu / Get Menu Item By Id`
- Expected status: `200`

#### C27. Update menu item

- Request: `Menu / Update Menu Item`
- Expected status: `200`

#### C28. Seed order item for kitchen complete test

1. Run [06_seed_order_item_for_kitchen_test.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/06_seed_order_item_for_kitchen_test.sql)
2. Copy the returned `order_item_id`
3. Put that value into Postman environment variable `orderItemId`

#### C29. Complete kitchen order item

- Request: `Kitchen / Complete Order Item As Admin`
- Expected status: `200`
- After success, inventory should decrease and a stock movement should be created

### Phase D. Extra negative tests you should also run

1. Wrong admin password on login -> expect `401`
2. Register customer with duplicate email -> expect `409`
3. Create employee with duplicate username -> expect `409`
4. Admin delete self -> expect `403`
5. Refresh without cookie -> expect `401`
6. Create menu item duplicate name in same branch/category -> expect `409`
7. Complete kitchen item twice -> expect `409`
8. Complete kitchen item with insufficient stock -> expect `409`

## 11. Suggested Manual DB Checks In MySQL Workbench

After important phases, run these queries in Workbench.

You can either run the snippets below one by one, or run [03_postman_db_check_queries.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/03_postman_db_check_queries.sql).

### Check roles

```sql
USE goldenheart_restaurant;

SELECT id, name, description, created_at, updated_at, deleted_at
FROM roles
ORDER BY id;
```

### Check users and profiles

```sql
USE goldenheart_restaurant;

SELECT
    u.id,
    u.username,
    u.status,
    u.role_id,
    u.created_at,
    u.updated_at,
    u.deleted_at,
    up.full_name,
    up.employee_code,
    up.email,
    up.phone,
    up.branch_id
FROM users u
LEFT JOIN user_profiles up ON up.user_id = u.id
ORDER BY u.id;
```

### Check refresh tokens

```sql
USE goldenheart_restaurant;

SELECT
    id,
    user_id,
    token_hash,
    expires_at,
    revoked,
    created_at,
    last_used_at,
    revoked_at
FROM refresh_tokens
ORDER BY id DESC;
```

### Check customers

```sql
USE goldenheart_restaurant;

SELECT
    id,
    customer_code,
    name,
    email,
    phone,
    loyalty_points,
    created_at,
    updated_at,
    deleted_at
FROM customers
ORDER BY id;
```

### Check menu items and recipes

```sql
USE goldenheart_restaurant;

SELECT
    mi.id,
    mi.branch_id,
    mi.category_id,
    mi.name,
    mi.price,
    mi.status
FROM menu_items mi
ORDER BY mi.id;

SELECT
    r.id,
    r.menu_item_id,
    r.ingredient_id,
    r.quantity
FROM recipes r
ORDER BY r.id;
```

### Check order items for kitchen test

```sql
USE goldenheart_restaurant;

SELECT
    o.id AS order_id,
    o.status AS order_status,
    oi.id AS order_item_id,
    oi.menu_item_id,
    oi.quantity,
    oi.status AS order_item_status,
    oi.note
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
ORDER BY oi.id;
```

### Check inventory after kitchen completion

```sql
USE goldenheart_restaurant;

SELECT
    i.id,
    i.branch_id,
    i.ingredient_id,
    i.quantity,
    i.average_unit_cost
FROM inventory i
ORDER BY i.id;

SELECT
    sm.id,
    sm.branch_id,
    sm.ingredient_id,
    sm.order_id,
    sm.order_item_id,
    sm.movement_type,
    sm.quantity_change,
    sm.balance_after,
    sm.total_cost,
    sm.occurred_at
FROM stock_movements sm
ORDER BY sm.id DESC;
```

## 12. Common Problems During Testing

### App does not start

- Cause: old schema mismatch from previous database
- Fix: rerun [01_reset_local_database.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/01_reset_local_database.sql), then start app again

### Refresh returns `401`

- Cause: missing or revoked refresh cookie
- Fix: login again first, then call refresh in the same Postman session

### Employee create returns `409`

- Cause: duplicate `username`, `email`, `phone`, or `employeeCode`
- Fix: change values in request body

### Customer create returns `409`

- Cause: duplicate `email`, `phone`, or `customerCode`
- Fix: change values in request body

### `403 Forbidden` on list/detail endpoints

- Cause: using `STAFF` or `CUSTOMER` token on admin/manager-only endpoints
- Fix: switch back to `adminAccessToken` or `managerAccessToken`

### Menu create returns `404`

- Cause: `branchId`, `categoryId` or `ingredientId` does not exist
- Fix: rerun [02_seed_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/02_seed_reference_data.sql) and [05_seed_menu_inventory_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/05_seed_menu_inventory_reference_data.sql)

### Kitchen complete returns `409`

- Possible causes:
  - order item already completed
  - order item was cancelled
  - menu item has no recipe
  - inventory not found
  - not enough stock
- Fix:
  - reseed order item with [06_seed_order_item_for_kitchen_test.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/06_seed_order_item_for_kitchen_test.sql)
  - verify inventory quantity in [03_postman_db_check_queries.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/03_postman_db_check_queries.sql)

## 13. Practical Runbook

This section is the fastest way to test the project from an empty database.

### 13.1 Data You Should Add First

Because some modules still do not have CRUD APIs for their master data, use SQL for the base records first.

Run in this order:

1. [01_reset_local_database.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/01_reset_local_database.sql)
2. Start backend
3. [02_seed_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/02_seed_reference_data.sql)
4. [05_seed_menu_inventory_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/05_seed_menu_inventory_reference_data.sql)
5. Optional: [04_optional_seed_customers.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/04_optional_seed_customers.sql)

After those scripts, you should have:

- `roles`: seeded automatically by app startup
- `users`: at least admin account
- `restaurants`: `id=1`
- `branches`: `id=1`, `id=2`
- `categories`: `id=1` for `Pho`, `id=2` for `Mi`
- `ingredients`: `id=1..4`
- `inventory`: stock records for branch `1`

### 13.2 Runbook Table

| Step | What to run | Type | Expected status | Seed/data needed first | Main tables affected |
| --- | --- | --- | --- | --- | --- |
| 1 | Reset DB | Workbench SQL | Success in Workbench | None | Drops and recreates database |
| 2 | Start backend | App run | App starts | Reset DB done | Auto-creates tables, seeds `roles`, creates admin user |
| 3 | Seed branches | Workbench SQL | Success in Workbench | Backend must have started once | `restaurants`, `branches` |
| 4 | Seed menu refs | Workbench SQL | Success in Workbench | Step 3 done | `categories`, `ingredients`, `inventory` |
| 5 | `Auth / Login Admin` | Postman | `200` | Step 2 done | Inserts 1 row in `refresh_tokens` |
| 6 | `Roles / Get Roles` | Postman | `200` | Step 5 done | No data change |
| 7 | `Employees / Create Manager By Admin` | Postman | `201` | Step 6 done | `users`, `user_profiles` |
| 8 | `Auth / Login Manager` | Postman | `200` | Step 7 done | Adds row in `refresh_tokens` |
| 9 | `Employees / Create Staff By Admin` | Postman | `201` | Step 6 done | `users`, `user_profiles` |
| 10 | `Auth / Login Staff` | Postman | `200` | Step 9 done | Adds row in `refresh_tokens` |
| 11 | `Employees / Staff Get My Profile` | Postman | `200` | Step 10 done | No data change |
| 12 | `Employees / Staff Update My Profile` | Postman | `200` | Step 10 done | Updates `user_profiles` |
| 13 | `Employees / Manager Create Staff Without RoleId` | Postman | `201` | Step 8 done | `users`, `user_profiles` |
| 14 | `Customers / Create Customer 01` | Postman | `201` | Step 5 done | `customers` |
| 15 | `Customers / Create Customer 02` | Postman | `201` | Step 8 done | `customers` |
| 16 | `Menu / Create Menu Item Pho Bo Tai` | Postman | `201` | Step 4 and Step 5 done | `menu_items`, `recipes` |
| 17 | `Menu / Get Menu Items` | Postman | `200` | Step 16 done | No data change |
| 18 | `Menu / Update Menu Item` | Postman | `200` | Step 16 done | Updates `menu_items`, replaces `recipes` |
| 19 | Seed order item for kitchen test | Workbench SQL | Success in Workbench | Step 16 done | `orders`, `order_items` |
| 20 | `Kitchen / Complete Order Item As Admin` | Postman | `200` | Step 19 done | Updates `order_items`, `orders`, `inventory`; inserts `stock_movements` |
| 21 | `Auth / Refresh Current Session` | Postman | `200` | A valid cookie session exists | Revokes old `refresh_tokens` row, inserts new one |
| 22 | `Auth / Logout Current Session` | Postman | `200` | A valid cookie session exists | Marks refresh token revoked |

### 13.3 Detailed Step-By-Step Runbook

#### Step 1. Reset database

Run:

- [01_reset_local_database.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/01_reset_local_database.sql)

Expected:

- Workbench runs without error
- database `goldenheart_restaurant` exists

Check:

```sql
SHOW DATABASES LIKE 'goldenheart_restaurant';
```

#### Step 2. Start backend

Expected:

- Spring Boot starts successfully
- no schema mismatch error

What should appear in DB right after app startup:

- `roles` contains `ADMIN`, `MANAGER`, `STAFF`, `KITCHEN`, `CUSTOMER`
- `users` contains `admin`
- `user_profiles` contains `System Admin`

Check:

```sql
USE goldenheart_restaurant;

SELECT id, name FROM roles ORDER BY id;

SELECT id, username, role_id, status FROM users ORDER BY id;

SELECT user_id, full_name, email FROM user_profiles ORDER BY user_id;
```

#### Step 3. Seed restaurant and branch data

Run:

- [02_seed_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/02_seed_reference_data.sql)

Expected:

- branch `1` exists
- branch `2` exists

Check:

```sql
SELECT id, restaurant_id, name FROM branches ORDER BY id;
```

#### Step 4. Seed category, ingredient and inventory data

Run:

- [05_seed_menu_inventory_reference_data.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/05_seed_menu_inventory_reference_data.sql)

Expected:

- category `1` = `Pho`
- category `2` = `Mi`
- ingredient ids `1,2,3,4` exist
- inventory exists for branch `1`

Check:

```sql
SELECT id, name FROM categories ORDER BY id;
SELECT id, name, unit FROM ingredients ORDER BY id;
SELECT id, branch_id, ingredient_id, quantity FROM inventory ORDER BY id;
```

#### Step 5. Login as admin

Run in Postman:

- `Auth / Login Admin`

Expected:

- status `200`
- response contains `accessToken`
- Postman saves `adminAccessToken`
- cookie jar stores refresh token

DB effect:

- 1 new row in `refresh_tokens`

Check:

```sql
SELECT id, user_id, revoked, expires_at
FROM refresh_tokens
ORDER BY id DESC;
```

#### Step 6. Get roles

Run in Postman:

- `Roles / Get Roles`

Expected:

- status `200`
- Postman stores role ids into environment

DB effect:

- no data change

#### Step 7. Create manager by admin

Run in Postman:

- `Employees / Create Manager By Admin`

Expected:

- status `201`
- response contains employee id
- Postman stores `managerId`

DB effect:

- new row in `users`
- new row in `user_profiles`

Check:

```sql
SELECT u.id, u.username, u.role_id, up.full_name, up.employee_code, up.email
FROM users u
JOIN user_profiles up ON up.user_id = u.id
WHERE u.username = 'manager01';
```

#### Step 8. Login manager

Run in Postman:

- `Auth / Login Manager`

Expected:

- status `200`
- Postman stores `managerAccessToken`

DB effect:

- new row in `refresh_tokens`

#### Step 9. Create staff by admin

Run in Postman:

- `Employees / Create Staff By Admin`

Expected:

- status `201`
- Postman stores `staffId`

DB effect:

- new row in `users`
- new row in `user_profiles`

#### Step 10. Login staff

Run in Postman:

- `Auth / Login Staff`

Expected:

- status `200`
- Postman stores `staffAccessToken`

DB effect:

- new row in `refresh_tokens`

#### Step 11. Staff views own profile

Run in Postman:

- `Employees / Staff Get My Profile`

Expected:

- status `200`
- returned profile does not contain salary

DB effect:

- no data change

#### Step 12. Staff updates own profile

Run in Postman:

- `Employees / Staff Update My Profile`

Expected:

- status `200`
- `fullName`, `email`, `phone`, `address` can change

DB effect:

- `user_profiles.updated_at` changes
- current employee email/phone values change

Check:

```sql
SELECT user_id, full_name, email, phone, address, updated_at
FROM user_profiles
WHERE user_id = {{staffId}};
```

In Workbench, replace `{{staffId}}` with the real id from Postman environment.

#### Step 13. Manager creates another staff without roleId

Run in Postman:

- `Employees / Manager Create Staff Without RoleId`

Expected:

- status `201`
- created employee gets default role `STAFF`

DB effect:

- new row in `users`
- new row in `user_profiles`

Check:

```sql
SELECT u.id, u.username, r.name AS role_name
FROM users u
JOIN roles r ON r.id = u.role_id
WHERE u.username = 'staff02';
```

#### Step 14. Create first customer

Run in Postman:

- `Customers / Create Customer 01`

Expected:

- status `201`
- Postman stores `customerId`

DB effect:

- new row in `customers`

#### Step 15. Create second customer

Run in Postman:

- `Customers / Create Customer 02`

Expected:

- status `201`

DB effect:

- another row in `customers`

#### Step 16. Create menu item

Run in Postman:

- `Menu / Create Menu Item Pho Bo Tai`

Expected:

- status `201`
- Postman stores `menuItemId`

DB effect:

- 1 new row in `menu_items`
- 4 new rows in `recipes`

Check:

```sql
SELECT id, branch_id, category_id, name, price, status
FROM menu_items
ORDER BY id;

SELECT id, menu_item_id, ingredient_id, quantity
FROM recipes
ORDER BY id;
```

#### Step 17. Get menu items

Run in Postman:

- `Menu / Get Menu Items`

Expected:

- status `200`
- `STAFF` token can read menu items

DB effect:

- no data change

#### Step 18. Update menu item

Run in Postman:

- `Menu / Update Menu Item`

Expected:

- status `200`
- name becomes `Pho Bo Tai Special`
- recipe quantities change

DB effect:

- row in `menu_items` updated
- old recipe rows removed and replaced with new recipe rows

#### Step 19. Seed order item for kitchen test

Run:

- [06_seed_order_item_for_kitchen_test.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/06_seed_order_item_for_kitchen_test.sql)

Important:

- this script looks for a menu item named `Pho Bo Tai`
- if you already renamed it to `Pho Bo Tai Special`, either:
  - run the kitchen seed before updating the menu item, or
  - temporarily change the script/menu item name to match

Expected:

- one `orders` row exists with status `PENDING`
- one `order_items` row exists with status `PENDING`

What to do next:

- copy returned `order_item_id`
- paste it into Postman environment variable `orderItemId`

#### Step 20. Complete order item in kitchen

Run in Postman:

- `Kitchen / Complete Order Item As Admin`

Expected:

- status `200`
- `order_item.status = COMPLETED`
- parent `order.status = PROCESSING` or `COMPLETED`
- inventory quantities decrease
- stock movement rows are inserted

Check:

```sql
SELECT
    o.id AS order_id,
    o.status AS order_status,
    oi.id AS order_item_id,
    oi.status AS order_item_status
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
ORDER BY oi.id DESC;

SELECT id, ingredient_id, quantity
FROM inventory
ORDER BY id;

SELECT id, order_item_id, ingredient_id, movement_type, quantity_change, balance_after
FROM stock_movements
ORDER BY id DESC;
```

#### Step 21. Refresh token

Run in Postman:

- `Auth / Refresh Current Session`

Expected:

- status `200`
- new access token returned

DB effect:

- current refresh token row becomes `revoked = true`
- new refresh token row is inserted

#### Step 22. Logout

Run in Postman:

- `Auth / Logout Current Session`

Expected:

- status `200`

DB effect:

- current refresh token row becomes revoked

### 13.4 Recommended Negative Test Order

Run these after the main happy path:

1. `Auth / Login Wrong Password`
   Expected: `401`
   DB effect: none
2. `Auth / Register Customer Duplicate Email`
   Expected: `409`
   DB effect: none
3. `Employees / Create Employee Duplicate Username`
   Expected: `409`
   DB effect: none
4. `Employees / Manager Create With RoleId Forbidden`
   Expected: `403`
   DB effect: none
5. `Employees / Manager Update Role Forbidden`
   Expected: `403`
   DB effect: none
6. `Employees / Staff Get Employees Forbidden`
   Expected: `403`
   DB effect: none
7. `Customers / Create Customer Duplicate Email`
   Expected: `409`
   DB effect: none
8. `Menu / Create Menu Duplicate Name Conflict`
   Expected: `409`
   DB effect: none
9. `Auth / Refresh Without Cookie`
   Expected: `401`
   DB effect: none
10. `Employees / Admin Delete Self Forbidden`
    Expected: `403`
    DB effect: none

### 13.5 Best Practical Order If You Want The Smoothest Session

Use this exact order:

1. Reset DB
2. Start backend
3. Seed reference data
4. Seed menu inventory reference data
5. Login admin
6. Get roles
7. Create manager
8. Login manager
9. Create staff by admin
10. Login staff
11. Create customers
12. Create menu item
13. Get menu items
14. Seed order item for kitchen
15. Complete kitchen order item
16. Run negative tests
17. Run DB check script [03_postman_db_check_queries.sql](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/sql/03_postman_db_check_queries.sql)
