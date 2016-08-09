-- #-- Example
--
-- # --- !Ups
-- CREATE TABLE billingplans (billingplanid serial, braintreetoken varchar(220), numberoftabs integer, startdate timestamp without time zone, current boolean, isdeleted boolean, price double precision, truckid integer)
--
-- # --- !Downs
-- DROP TABLE billingplans

--REFERENCES tableName(columnName)
