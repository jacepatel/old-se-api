-- #-- Example
--
-- # --- !Ups
-- ALTER TABLE Trucks ADD COLUMN SMSMessage varchar(250) Default NULL;
-- ALTER TABLE Events ADD COLUMN ParentId INT Default NULL;
-- # --- !Downs
-- ALTER TABLE Trucks DROP COLUMN SMSMessage;
-- ALTER TABLE Events DROP COLUMN ParentID;
