-- #-- Example
--
-- # --- !Ups
-- ALTER TABLE Orders ADD COLUMN acceptedTime timestamp;
--
-- # --- !Downs
-- ALTER TABLE Orders DROP COLUMN acceptedTime;