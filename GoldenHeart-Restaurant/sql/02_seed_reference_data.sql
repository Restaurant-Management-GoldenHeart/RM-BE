USE goldenheart_restaurant;

INSERT INTO restaurants (id, name, address, phone)
SELECT 1, 'Golden Heart Main Restaurant', '123 Nguyen Hue, District 1, HCM City', '02873001234'
WHERE NOT EXISTS (
    SELECT 1
    FROM restaurants
    WHERE id = 1
);

INSERT INTO branches (id, restaurant_id, name, address, phone)
SELECT 1, 1, 'Golden Heart Branch 1', '45 Le Loi, District 1, HCM City', '02873000001'
WHERE EXISTS (
      SELECT 1
      FROM restaurants r
      WHERE r.id = 1
  )
  AND NOT EXISTS (
      SELECT 1
      FROM branches b
      WHERE b.id = 1
  );

INSERT INTO branches (id, restaurant_id, name, address, phone)
SELECT 2, 1, 'Golden Heart Branch 2', '88 Vo Van Ngan, Thu Duc, HCM City', '02873000002'
WHERE EXISTS (
      SELECT 1
      FROM restaurants r
      WHERE r.id = 1
  )
  AND NOT EXISTS (
      SELECT 1
      FROM branches b
      WHERE b.id = 2
  );

SELECT id, name, address, phone
FROM restaurants
ORDER BY id;

SELECT id, restaurant_id, name, address, phone
FROM branches
ORDER BY id;
