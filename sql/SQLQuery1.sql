-- Create the database
CREATE DATABASE dbThobaPlug;
GO

-- Users table
CREATE TABLE Userr (
    userr_id     INT IDENTITY(1,1) PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at  DATETIME DEFAULT GETDATE()
);
GO

-- Messages table
CREATE TABLE Messagee (
    message_id  INT IDENTITY(1,1) PRIMARY KEY,
    sender_id   INT NOT NULL,
    recipient_id INT NULL,
    content     NVARCHAR(1000) NOT NULL,
    sent_at     DATETIME DEFAULT GETDATE(),
    is_private  BIT DEFAULT 0,
    FOREIGN KEY (sender_id) REFERENCES Userr(userr_id),
    FOREIGN KEY (recipient_id) REFERENCES Userr(userr_id)
);
GO

USE dbThobaPlug;
DELETE FROM Messagee;
DELETE FROM Userr;

USE dbThobaPlug;
SELECT * FROM Messagee;


USE master;
ALTER LOGIN ndabane_user ENABLE;
ALTER LOGIN ndabane_user WITH PASSWORD = 'Admin123!';

USE master;
SELECT name, is_disabled FROM sys.server_principals WHERE name = 'ndabane_user';
USE master;
ALTER LOGIN ndabane_user WITH PASSWORD = 'Admin123!';

ALTER LOGIN ndabane_user ENABLE;
ALTER LOGIN ndabane_user WITH PASSWORD = 'Admin123!';