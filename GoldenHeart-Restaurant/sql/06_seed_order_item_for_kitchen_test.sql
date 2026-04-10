USE goldenheart_restaurant;

INSERT INTO orders (
    branch_id,
    table_id,
    customer_id,
    created_by,
    status,
    created_at,
    closed_at
)
SELECT
    1,
    NULL,
    NULL,
    1,
    'PENDING',
    NOW(),
    NULL
WHERE EXISTS (
        SELECT 1
        FROM menu_items mi
        WHERE mi.branch_id = 1
          AND mi.name IN ('Pho Bo Tai', 'Pho Bo Tai Special')
    )
  AND NOT EXISTS (
        SELECT 1
        FROM orders o
        WHERE o.branch_id = 1
          AND o.created_by = 1
          AND o.status = 'PENDING'
    );

INSERT INTO order_items (
    order_id,
    menu_item_id,
    quantity,
    price,
    status,
    note
)
SELECT
    o.id,
    mi.id,
    2,
    mi.price,
    'PENDING',
    'Seeded for kitchen complete API test'
FROM orders o
JOIN menu_items mi
    ON mi.branch_id = 1
   AND mi.name IN ('Pho Bo Tai', 'Pho Bo Tai Special')
WHERE o.branch_id = 1
  AND o.created_by = 1
  AND o.status = 'PENDING'
  AND NOT EXISTS (
      SELECT 1
      FROM order_items oi
      WHERE oi.order_id = o.id
        AND oi.menu_item_id = mi.id
        AND oi.note = 'Seeded for kitchen complete API test'
  );

SELECT
    o.id AS order_id,
    oi.id AS order_item_id,
    mi.name AS menu_item_name,
    oi.quantity,
    oi.status,
    oi.note
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
JOIN menu_items mi ON mi.id = oi.menu_item_id
WHERE oi.note = 'Seeded for kitchen complete API test'
ORDER BY oi.id DESC;
