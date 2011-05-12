SET NUMERIC_ROUNDABORT OFF
GO
SET ANSI_PADDING,ANSI_WARNINGS,CONCAT_NULL_YIELDS_NULL,ARITHABORT,QUOTED_IDENTIFIER,ANSI_NULLS ON
GO

CREATE DATABASE [${%%OPENIDM_DATABASE%%}]
GO

--For SQL Server authentication:
-- sp_addlogin <user>, <password>, <defaultdb>
--For Windows authentication:
-- sp_grantlogin <domain\user>
--For SQL Server 2005:
--CREATE LOGIN [OPENIDM] WITH PASSWORD=N'password', DEFAULT_DATABASE=[${%%OPENIDM_DATABASE%%}], CHECK_EXPIRATION=OFF, CHECK_POLICY=OFF
--CREATE LOGIN [OPENIDM_PROXY] WITH PASSWORD=N'password', DEFAULT_DATABASE=[${%%OPENIDM_DATABASE%%}], CHECK_EXPIRATION=OFF, CHECK_POLICY=OFF
--sp_addlogin 'OPENIDM_PROXY', 'Passw0rd', 'OpenIDM'
GO


USE [${%%OPENIDM_DATABASE%%}]
GO

--For SQL Server 2005 SP2 create a schema - not needed in other versions:
CREATE SCHEMA OpenIDM
GO

--For SQL Server 2005 SP2 use CREATE user instead of sp_grantdbaccess
CREATE USER OpenIDM FOR LOGIN OPENIDM_PROXY with DEFAULT_SCHEMA = OpenIDM
--sp_grantdbaccess 'OpenIDM'
GO

--Should not be left in; just run as a user with dbo rights
--sp_addrolemember 'db_owner','OPENIDM'
--GO

BEGIN TRANSACTION


CREATE TABLE [OpenIDM].[Schemas]
( UUID              VARCHAR(36)    	CONSTRAINT PK_Schemas PRIMARY KEY
--, targer_id          		INT					NOT NULL CONSTRAINT FK_Roles_resource_id		REFERENCES [OpenIDM].[Resources](resource_id)
, targer_name				NVARCHAR(448)   	NOT NULL 
, URI             			NVARCHAR(450)    	NOT NULL
, version					INT					NOT NULL DEFAULT 1
, data_XML					XML					NOT NULL
, CONSTRAINT UQ_Schemas_targer_id UNIQUE (targer_name,version)
)
GO
CREATE INDEX Schemas_targer_name		ON [OpenIDM].[Schemas](targer_name)
GO
GRANT SELECT, INSERT, UPDATE ON [OpenIDM].[Schemas] TO OpenIDM
GO


COMMIT
GO
