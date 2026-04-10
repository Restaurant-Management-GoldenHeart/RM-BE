USE goldenheart_restaurant;

INSERT INTO customers (
    customer_code,
    name,
    phone,
    email,
    active_phone,
    active_email,
    loyalty_points,
    address,
    date_of_birth,
    gender,
    note,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'CUS901',
    'Pham Van Test 1',
    '0933333301',
    'seed.customer1@example.com',
    '0933333301',
    'seed.customer1@example.com',
    10,
    'HCM City',
    '1999-01-01',
    'Male',
    'Seeded from MySQL Workbench',
    NOW(),
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM customers WHERE customer_code = 'CUS901'
);

INSERT INTO customers (
    customer_code,
    name,
    phone,
    email,
    active_phone,
    active_email,
    loyalty_points,
    address,
    date_of_birth,
    gender,
    note,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'CUS902',
    'Le Thi Test 2',
    '0933333302',
    'seed.customer2@example.com',
    '0933333302',
    'seed.customer2@example.com',
    20,
    'Da Nang',
    '1998-02-02',
    'Female',
    'Seeded from MySQL Workbench',
    NOW(),
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM customers WHERE customer_code = 'CUS902'
);

INSERT INTO customers (
    customer_code,
    name,
    phone,
    email,
    active_phone,
    active_email,
    loyalty_points,
    address,
    date_of_birth,
    gender,
    note,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'CUS903',
    'Nguyen Test 3',
    '0933333303',
    'seed.customer3@example.com',
    '0933333303',
    'seed.customer3@example.com',
    30,
    'Ha Noi',
    '1997-03-03',
    'Male',
    'Seeded from MySQL Workbench',
    NOW(),
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM customers WHERE customer_code = 'CUS903'
);

SELECT
    id,
    customer_code,
    name,
    phone,
    email,
    loyalty_points,
    created_at,
    updated_at
FROM customers
ORDER BY id;
