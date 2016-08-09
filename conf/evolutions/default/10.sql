-- #-- Example
--
-- # --- !Ups
-- ALTER TABLE TruckSEssions ADD COLUMN IsUsed BOOLEAN Default NULL;
-- ALTER TABLE TruckSEssions ADD COLUMN IsDeleted BOOLEAN Default NULL;
-- # --- !Downs
-- ALTER TABLE TruckSEssions DROP COLUMN IsUsed;
-- ALTER TABLE TruckSEssions DROP COLUMN IsDeleted;

