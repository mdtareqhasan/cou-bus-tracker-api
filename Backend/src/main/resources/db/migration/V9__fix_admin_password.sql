-- V9: Fix admin password hash (correct BCrypt hash for Admin@123)
UPDATE admins SET password = '$2b$10$CqtkmaHceRdasMd7kjzfQ.kw3TI/lvuabJc5i4laQOOjqIdAHWYXe' WHERE email = 'admin@cou.ac.bd';
