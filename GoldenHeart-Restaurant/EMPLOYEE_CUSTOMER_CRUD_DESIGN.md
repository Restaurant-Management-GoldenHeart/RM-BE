# Employee & Customer CRUD Design

## Muc tieu

Tai lieu nay chot thiet ke product-ready cho CRUD Nhan vien va Khach hang, bao gom:

- schema database
- logic RBAC
- query SQL an toan
- DTO output de an truong nhay cam
- validation o server side

## 1. Mo hinh du lieu de dung

### Employee

Trong du an hien tai, nhan vien nen duoc model theo aggregate:

- `users`: tai khoan dang nhap, role, status, audit
- `user_profiles`: thong tin nghiep vu cua nhan vien

Phan tach nhu vay hop ly hon viec tao bang `employees` rieng vi:

- phu hop voi auth hien co
- role va session da dua tren `users`
- staff/kitchen cap nhat thong tin ca nhan theo `user_id` rat tu nhien

### Customer

Khach hang dung bang `customers` rieng vi:

- khong bat buoc co tai khoan dang nhap
- co loyalty points, lich su order, thong tin cham soc khach hang

## 2. RBAC cho CRUD

### Admin

- full CRUD `users`, `user_profiles`, `customers`
- duoc phep doi `role_id`
- duoc xem day du salary va thong tin nhay cam can thiet cho quan tri

### Manager

- Create / Read / Update nhan vien
- Create / Read / Update khach hang
- khong duoc soft delete nhan vien hay khach hang
- role la view-only, khong duoc doi `role_id`
- co the xem salary neu nghiep vu yeu cau, neu khong thi nen an o API list

### Staff / Kitchen

- khong duoc xem danh sach nhan vien khac
- chi duoc `GET /me` va `PUT /me`
- khong duoc doi role, salary, employee code, branch assignment
- co the xem role cua chinh minh o muc read-only
- khach hang: chi duoc quyen theo vai tro nghiep vu neu sau nay ban mo rong, nhung hien tai khong nen cho CRUD nhan vien

## 3. Nguyen tac product-ready

1. Tat ca query danh sach va chi tiet deu phai loc `deleted_at IS NULL`
2. Khong hard delete trong CRUD thong thuong
3. Khong bao gio tra `password_hash`
4. Khong tra `salary` khi actor la staff/kitchen hoac khi xem nguoi khac
5. Tat ca query deu dung prepared statement / named parameter
6. Validation phai chay truoc khi goi query

## 4. Schema Database de xuat

Luu y: vi project dang dung MySQL, de vua soft delete vua giu unique cho ban ghi active, nen dung generated column cho email/phone active.

```sql
CREATE TABLE roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL
);

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE user_profiles (
    user_id INT PRIMARY KEY,
    employee_code VARCHAR(30) NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NOT NULL,
    active_phone VARCHAR(20) GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN phone ELSE NULL END
    ) STORED,
    active_email VARCHAR(100) GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN email ELSE NULL END
    ) STORED,
    branch_id INT NULL,
    address VARCHAR(255),
    date_of_birth DATE NULL,
    gender VARCHAR(10) NULL,
    hire_date DATE NULL,
    salary DECIMAL(12,2) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_profiles_branch FOREIGN KEY (branch_id) REFERENCES branches(id),
    CONSTRAINT uk_user_profiles_active_phone UNIQUE (active_phone),
    CONSTRAINT uk_user_profiles_active_email UNIQUE (active_email)
);

CREATE TABLE customers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(30) NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NULL,
    active_phone VARCHAR(20) GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN phone ELSE NULL END
    ) STORED,
    active_email VARCHAR(100) GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN email ELSE NULL END
    ) STORED,
    loyalty_points INT NOT NULL DEFAULT 0,
    address VARCHAR(255),
    date_of_birth DATE NULL,
    gender VARCHAR(10) NULL,
    note VARCHAR(500) NULL,
    last_visit_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    CONSTRAINT uk_customers_active_phone UNIQUE (active_phone),
    CONSTRAINT uk_customers_active_email UNIQUE (active_email)
);
```

## 5. Query SQL mau theo RBAC

Tat ca query duoi day deu gia dinh su dung prepared statement.

### 5.1 Admin list employees

```sql
SELECT
    u.id,
    u.username,
    u.status,
    r.id AS role_id,
    r.name AS role_name,
    up.full_name,
    up.employee_code,
    up.email,
    up.phone,
    up.salary,
    b.id AS branch_id,
    b.name AS branch_name,
    u.created_at,
    u.updated_at
FROM users u
JOIN user_profiles up ON up.user_id = u.id
JOIN roles r ON r.id = u.role_id
LEFT JOIN branches b ON b.id = up.branch_id
WHERE u.deleted_at IS NULL
  AND up.deleted_at IS NULL
  AND r.deleted_at IS NULL
ORDER BY u.id DESC
LIMIT :limit OFFSET :offset;
```

### 5.2 Manager update employee

Manager duoc update thong tin nhan vien, nhung khong duoc doi `role_id`.

```sql
UPDATE user_profiles up
JOIN users u ON u.id = up.user_id
SET
    up.full_name = :fullName,
    up.email = :email,
    up.phone = :phone,
    up.address = :address,
    up.branch_id = :branchId,
    up.date_of_birth = :dateOfBirth,
    up.gender = :gender,
    up.hire_date = :hireDate,
    up.salary = :salary,
    up.updated_at = CURRENT_TIMESTAMP
WHERE up.user_id = :employeeUserId
  AND up.deleted_at IS NULL
  AND u.deleted_at IS NULL;
```

Neu muon cho manager update `users.status`:

```sql
UPDATE users
SET
    status = :status,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :employeeUserId
  AND deleted_at IS NULL;
```

### 5.3 Staff/Kitchen xem thong tin chinh minh

```sql
SELECT
    u.id,
    u.username,
    u.status,
    r.name AS role_name,
    up.full_name,
    up.employee_code,
    up.email,
    up.phone,
    up.address,
    up.date_of_birth,
    up.gender,
    up.hire_date,
    b.id AS branch_id,
    b.name AS branch_name,
    u.created_at,
    u.updated_at
FROM users u
JOIN user_profiles up ON up.user_id = u.id
JOIN roles r ON r.id = u.role_id
LEFT JOIN branches b ON b.id = up.branch_id
WHERE u.id = :sessionUserId
  AND u.deleted_at IS NULL
  AND up.deleted_at IS NULL
  AND r.deleted_at IS NULL;
```

Query nay co chu y:

- khong select `password_hash`
- khong select `salary`

### 5.4 Staff/Kitchen update chinh minh

```sql
UPDATE user_profiles
SET
    full_name = :fullName,
    email = :email,
    phone = :phone,
    address = :address,
    date_of_birth = :dateOfBirth,
    gender = :gender,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = :sessionUserId
  AND deleted_at IS NULL;
```

Khong cho update:

- `salary`
- `employee_code`
- `branch_id`
- `role_id`
- `status`

### 5.5 Admin soft delete employee

Nen soft delete ca `users` va `user_profiles` trong mot transaction.

```sql
UPDATE users
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :employeeUserId
  AND deleted_at IS NULL;

UPDATE user_profiles
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = :employeeUserId
  AND deleted_at IS NULL;
```

### 5.6 Admin / Manager list customers

```sql
SELECT
    c.id,
    c.customer_code,
    c.name,
    c.phone,
    c.email,
    c.loyalty_points,
    c.address,
    c.last_visit_at,
    c.created_at,
    c.updated_at
FROM customers c
WHERE c.deleted_at IS NULL
  AND (:keyword IS NULL
       OR c.name LIKE CONCAT('%', :keyword, '%')
       OR c.phone LIKE CONCAT('%', :keyword, '%')
       OR c.email LIKE CONCAT('%', :keyword, '%'))
ORDER BY c.id DESC
LIMIT :limit OFFSET :offset;
```

### 5.7 Manager update customer

```sql
UPDATE customers
SET
    name = :name,
    phone = :phone,
    email = :email,
    address = :address,
    date_of_birth = :dateOfBirth,
    gender = :gender,
    note = :note,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :customerId
  AND deleted_at IS NULL;
```

### 5.8 Admin soft delete customer

Manager khong duoc delete.

```sql
UPDATE customers
SET
    deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :customerId
  AND deleted_at IS NULL;
```

## 6. DTO output de tranh lo du lieu

### EmployeeResponse

Dung cho Admin / Manager. Co the chua salary neu actor duoc phep xem.

### EmployeeSelfResponse

Dung cho Staff / Kitchen khi xem ho so cua minh. Khong co:

- `password_hash`
- `salary`

### CustomerResponse

Khong can tra nhung thong tin noi bo ngoai nghiep vu khach hang.

## 7. Validation o backend

Server side validation nen chay truoc query:

- username: bat buoc, toi da 50 ky tu
- password: toi thieu 8 ky tu, co chu va so
- email: dung dinh dang
- phone: dung regex cho so dien thoai
- salary: khong am
- date_of_birth: phai la ngay trong qua khu

Ngoai annotation validation, service van phai check:

- email active co trung khong
- phone active co trung khong
- role co ton tai khong
- branch co ton tai khong
- actor co du quyen sua field nay khong

## 8. API goi y theo MVC webservice

### Employee

- `GET /api/v1/employees`
- `GET /api/v1/employees/{id}`
- `POST /api/v1/employees`
- `PUT /api/v1/employees/{id}`
- `DELETE /api/v1/employees/{id}` soft delete, chi Admin
- `GET /api/v1/employees/me`
- `PUT /api/v1/employees/me`

### Customer

- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `POST /api/v1/customers`
- `PUT /api/v1/customers/{id}`
- `DELETE /api/v1/customers/{id}` soft delete, chi Admin

## 9. Ghi chu implement

1. Trong service, khong map truc tiep entity ra JSON
2. Luon map sang DTO
3. Salary chi map vao DTO khi actor co quyen
4. Tat ca repository custom query phai loc `deleted_at IS NULL`
5. Neu dung JPA, nen viet method query theo actor thay vi load het roi loc trong Java
