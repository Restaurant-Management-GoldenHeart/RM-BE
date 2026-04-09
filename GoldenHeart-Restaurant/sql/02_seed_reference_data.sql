USE goldenheart_restaurant;

INSERT INTO restaurants (name, address, phone)
SELECT 'Golden Heart Main Restaurant', '123 Nguyen Hue, District 1, HCM City', '02873001234'
WHERE NOT EXISTS (
    SELECT 1
    FROM restaurants
    WHERE name = 'Golden Heart Main Restaurant'
);

INSERT INTO branches (restaurant_id, name, address, phone)
SELECT r.id, 'Golden Heart Branch 1', '45 Le Loi, District 1, HCM City', '02873000001'
FROM restaurants r
WHERE r.name = 'Golden Heart Main Restaurant'
  AND NOT EXISTS (
      SELECT 1
      FROM branches b
      WHERE b.name = 'Golden Heart Branch 1'
  );

INSERT INTO branches (restaurant_id, name, address, phone)
SELECT r.id, 'Golden Heart Branch 2', '88 Vo Van Ngan, Thu Duc, HCM City', '02873000002'
FROM restaurants r
WHERE r.name = 'Golden Heart Main Restaurant'
  AND NOT EXISTS (
      SELECT 1
      FROM branches b
      WHERE b.name = 'Golden Heart Branch 2'
  );

SELECT id, name, address, phone
FROM restaurants
ORDER BY id;

SELECT id, restaurant_id, name, address, phone
FROM branches
ORDER BY id;
