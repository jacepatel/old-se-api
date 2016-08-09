-- #-- Example
--
-- # --- !Ups

--ALTER TABLE TruckSessions ADD COLUMN MaximumOrders INT
--ALTER TABLE TruckSEssions ADD COLUMN MinimumOrderValue MONEY

-- # --- !Downs
-- ALTER TABLE TruckSessions DROP COLUMN MaximumOrders
-- ALTER TABLE TruckSessions DROP COLUMN MinimumOrderValue
