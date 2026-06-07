INSERT INTO orders (id, customer_name, customer_email, idempotency_key, status, total_amount, deleted, version, created_at, updated_at)
VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'James Anderson', 'james.anderson@gmail.com', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PENDING', 1299.98, false, 0, NOW(), NOW()),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'Priya Patel', 'priya.patel@outlook.com', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'PENDING', 249.98, false, 0, NOW(), NOW()),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Michael Thompson', 'michael.thompson@yahoo.com', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'PROCESSING', 2399.97, false, 1, NOW(), NOW()),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'Emily Rodriguez', 'emily.rodriguez@gmail.com', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'SHIPPED', 1049.98, false, 2, NOW(), NOW()),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567805', 'David Chen', 'david.chen@gmail.com', 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'DELIVERED', 349.98, false, 3, NOW(), NOW()),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567806', 'Sophie Williams', 'sophie.williams@hotmail.com', 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'DELIVERED', 799.99, false, 3, NOW(), NOW()),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567807', 'Ryan Martinez', 'ryan.martinez@gmail.com', 'a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'CANCELLED', 1099.99, true, 1, NOW(), NOW());

INSERT INTO order_items (id, order_id, product_name, product_id, quantity, product_price)
VALUES
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567801', 'a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'Apple MacBook Air M2 13 inch 256GB Space Grey', 'APPL-MBA-M2-256-SGR', 1, 1199.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567802', 'a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'Targus 13 inch Laptop Bag Black', 'TARG-LB-13-BLK', 1, 99.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567803', 'a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'Samsung Galaxy Buds2 Pro Graphite', 'SAMS-GBP-GRA', 1, 179.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567804', 'a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'Anker USB-C to USB-C Braided Cable 2m', 'ANKR-USBC-2M', 1, 69.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567805', 'a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Sony PlayStation 5 Console Disc Edition', 'SONY-PS5-DISC', 1, 499.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567806', 'a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Samsung 27 inch 4K Gaming Monitor', 'SAMS-MON-27-4K', 1, 699.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567807', 'a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Logitech G Pro X Gaming Headset', 'LOGI-GPX-HEAD', 1, 1199.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567808', 'a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'Apple iPhone 15 Pro 256GB Natural Titanium', 'APPL-IPH15P-256-NAT', 1, 999.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567809', 'a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'Apple MagSafe Charger 15W', 'APPL-MAGSAFE-15W', 1, 49.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567810', 'a1b2c3d4-e5f6-7890-abcd-ef1234567805', 'Apple Watch Series 9 GPS 45mm Midnight', 'APPL-AW9-45-MID', 1, 299.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567811', 'a1b2c3d4-e5f6-7890-abcd-ef1234567805', 'Apple Watch Sport Band Midnight 45mm', 'APPL-AWB-45-MID', 1, 49.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567812', 'a1b2c3d4-e5f6-7890-abcd-ef1234567806', 'Apple iPad Pro 11 inch M2 256GB Space Grey WiFi', 'APPL-IPDP-11-256-SGR', 1, 799.99),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567813', 'a1b2c3d4-e5f6-7890-abcd-ef1234567807', 'Sony Alpha A7 III Mirrorless Camera Body', 'SONY-A7III-BODY', 1, 1099.99);