-- V11: Rename SAT-THU to SUN-THU in schedules
UPDATE schedules
SET days = 'SUN-THU'
WHERE days = 'SAT-THU';
