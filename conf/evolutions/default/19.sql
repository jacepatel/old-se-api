-- -
-- CREATE TABLE EventManagers (eventmanagerid bigserial primary key, username varchar(50), password varchar(80), name varchar(30),
-- contactemail varchar(120), contactnumber Varchar(30), isDeleted boolean default false, createddate timestamp)
--
--
-- ALTER TABLE EVents ADD Column EventManagerId INT REFERENCES EventManagers (eventManagerId) DEFAULT NULL
--