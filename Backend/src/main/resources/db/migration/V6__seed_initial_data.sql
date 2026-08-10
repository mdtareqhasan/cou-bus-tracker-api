-- V6: Seed initial data

-- Blue Buses
INSERT INTO buses (bus_number, category, route, is_active) VALUES
('BUS 03', 'BLUE', 'Kandirpar → Campus via Policeline', true),
('BUS 04', 'BLUE', 'Kandirpar → Campus via Policeline', true),
('BUS 05', 'BLUE', 'Kandirpar → Campus via Policeline', true),
('BUS 11', 'BLUE', 'Dhormopur → Campus', true),
('BUS 25', 'BLUE', 'Campus → Policeline → Eidgah', true),
('BUS 31', 'BLUE', 'Kandirpar → Campus', true),
('BUS 32', 'BLUE', 'Kandirpar → Campus via Tomsombridge', true),
('BUS 33', 'BLUE', 'Kandirpar → Campus via Tomsombridge', true);

-- Teacher Buses
INSERT INTO buses (bus_number, category, route, is_active) VALUES
('BUS 06', 'TEACHER', 'Policeline → Campus via Tomsombridge', true),
('BUS 08', 'TEACHER', 'Kandirpar → Campus', true);

-- Staff Bus
INSERT INTO buses (bus_number, category, route, is_active) VALUES
('BUS 14', 'STAFF', 'Kandirpar → Campus', true);

-- Admin account (password: Admin@123)
INSERT INTO admins (email, password, name) VALUES
('admin@cou.ac.bd', '$2b$10$CqtkmaHceRdasMd7kjzfQ.kw3TI/lvuabJc5i4laQOOjqIdAHWYXe', 'COU Admin');
