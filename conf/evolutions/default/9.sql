-- #-- Example
--
-- # --- !Ups
-- ALTER TABLE Items ADD COLUMN stockLevel INT DEFAULT NULL;
-- ALTER TABLE Orders ADD COLUMN Discount MONEY DEFAULT NULL
-- # --- !Downs
-- ALTER TABLE Items DROP COLUMN stockLevel;
--ALTER TABLE Orders DROP COLUMN Discount;