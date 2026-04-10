USE goldenheart_restaurant;

INSERT INTO categories (id, name, description)
SELECT 1, 'Pho', 'Vietnamese pho dishes'
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE id = 1
);

INSERT INTO categories (id, name, description)
SELECT 2, 'Mi', 'Noodle dishes'
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE id = 2
);

INSERT INTO ingredients (id, name, unit)
SELECT 1, 'Beef', 'gram'
WHERE NOT EXISTS (
    SELECT 1 FROM ingredients WHERE id = 1
);

INSERT INTO ingredients (id, name, unit)
SELECT 2, 'Rice Noodle', 'gram'
WHERE NOT EXISTS (
    SELECT 1 FROM ingredients WHERE id = 2
);

INSERT INTO ingredients (id, name, unit)
SELECT 3, 'Broth', 'ml'
WHERE NOT EXISTS (
    SELECT 1 FROM ingredients WHERE id = 3
);

INSERT INTO ingredients (id, name, unit)
SELECT 4, 'Onion', 'gram'
WHERE NOT EXISTS (
    SELECT 1 FROM ingredients WHERE id = 4
);

INSERT INTO inventory (
    id,
    branch_id,
    ingredient_id,
    quantity,
    min_stock_level,
    reorder_level,
    average_unit_cost,
    last_receipt_at,
    last_counted_at
)
SELECT 1, 1, 1, 5000.00, 500.00, 1000.00, 0.25, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE id = 1);

INSERT INTO inventory (
    id,
    branch_id,
    ingredient_id,
    quantity,
    min_stock_level,
    reorder_level,
    average_unit_cost,
    last_receipt_at,
    last_counted_at
)
SELECT 2, 1, 2, 8000.00, 800.00, 1500.00, 0.05, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE id = 2);

INSERT INTO inventory (
    id,
    branch_id,
    ingredient_id,
    quantity,
    min_stock_level,
    reorder_level,
    average_unit_cost,
    last_receipt_at,
    last_counted_at
)
SELECT 3, 1, 3, 20000.00, 2000.00, 5000.00, 0.01, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE id = 3);

INSERT INTO inventory (
    id,
    branch_id,
    ingredient_id,
    quantity,
    min_stock_level,
    reorder_level,
    average_unit_cost,
    last_receipt_at,
    last_counted_at
)
SELECT 4, 1, 4, 3000.00, 300.00, 600.00, 0.02, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM branches WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE id = 4);

SELECT id, name, description
FROM categories
ORDER BY id;

SELECT id, name, unit
FROM ingredients
ORDER BY id;

SELECT id, branch_id, ingredient_id, quantity, average_unit_cost
FROM inventory
ORDER BY id;

SELECT 'Use branchId=1, categoryId=1, ingredientIds=1,2,3,4 in Postman menu tests' AS note;
