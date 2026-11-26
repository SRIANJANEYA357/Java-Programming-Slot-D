USE foodsharedb;
INSERT INTO users (email, password, full_name, phone, role, city, created_at) 
VALUES ('admin@foodshare.com', '$2a$12$hhD7Li3VDkpC5GCqG9BwJOcGhriR2m59o8HAyc7DiaWCy2KN1TYD6', 'FoodShare Administrator', '9000000000', 'ROLE_ADMIN', 'Hyderabad', NOW());
