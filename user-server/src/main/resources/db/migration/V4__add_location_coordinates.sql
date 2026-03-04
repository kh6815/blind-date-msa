ALTER TABLE users ADD COLUMN latitude DOUBLE;
ALTER TABLE users ADD COLUMN longitude DOUBLE;

-- Update existing users with random coordinates near Seoul (approx 37.5665, 126.9780)
UPDATE users 
SET latitude = 37.5665 + (RAND() - 0.5) * 0.2,
    longitude = 126.9780 + (RAND() - 0.5) * 0.2
WHERE latitude IS NULL;
