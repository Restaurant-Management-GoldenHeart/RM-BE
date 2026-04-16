/*
  Reset only operational runtime data so you can rerun:
  - tables
  - orders
  - kitchen
  - billing

  This script intentionally keeps:
  - seeded reference/master data
  - seeded manager/staff/kitchen accounts
  - Postman-created customers/employees/menu/inventory CRUD records

  Use the full reset sequence (01 -> start app -> 02) when you want a completely clean rerun.
*/

USE goldenheart_restaurant;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM payments;
DELETE FROM bills;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM stock_movements
WHERE order_id IS NOT NULL
   OR order_item_id IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE bills AUTO_INCREMENT = 1;
ALTER TABLE order_items AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;

UPDATE tables
SET status = CASE id
    WHEN 1 THEN 'AVAILABLE'
    WHEN 2 THEN 'AVAILABLE'
    WHEN 3 THEN 'AVAILABLE'
    WHEN 4 THEN 'AVAILABLE'
    WHEN 5 THEN 'AVAILABLE'
    WHEN 6 THEN 'AVAILABLE'
    ELSE status
END;

UPDATE inventory
SET quantity = CASE id
        WHEN 1 THEN 15.00
        WHEN 2 THEN 25.00
        WHEN 3 THEN 60.00
        WHEN 4 THEN 5000.00
        WHEN 5 THEN 12.00
        WHEN 6 THEN 3000.00
        WHEN 7 THEN 0.00
        ELSE quantity
    END,
    min_stock_level = CASE id
        WHEN 1 THEN 2.00
        WHEN 2 THEN 3.00
        WHEN 3 THEN 10.00
        WHEN 4 THEN 500.00
        WHEN 5 THEN 2.00
        WHEN 6 THEN 200.00
        WHEN 7 THEN 1.00
        ELSE min_stock_level
    END,
    reorder_level = CASE id
        WHEN 1 THEN 4.00
        WHEN 2 THEN 5.00
        WHEN 3 THEN 20.00
        WHEN 4 THEN 1000.00
        WHEN 5 THEN 4.00
        WHEN 6 THEN 400.00
        WHEN 7 THEN 2.00
        ELSE reorder_level
    END,
    average_unit_cost = CASE id
        WHEN 1 THEN 220000.00
        WHEN 2 THEN 45000.00
        WHEN 3 THEN 15000.00
        WHEN 4 THEN 30.00
        WHEN 5 THEN 160000.00
        WHEN 6 THEN 45.00
        WHEN 7 THEN 28000.00
        ELSE average_unit_cost
    END,
    active_record_key = 'ACTIVE',
    deleted_at = NULL,
    updated_at = NOW(),
    last_counted_at = NULL
WHERE id BETWEEN 1 AND 7;

UPDATE customers
SET last_visit_at = NULL,
    updated_at = NOW()
WHERE id = 1;

SELECT
    'Operational data reset completed.' AS status,
    'You can rerun the login + tables + order + kitchen + billing folders now.' AS next_step;
