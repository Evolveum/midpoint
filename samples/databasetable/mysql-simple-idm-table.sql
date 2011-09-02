/*!40101 SET NAMES utf8 */;
/*!40101 SET character_set_client = utf8 */;

CREATE TABLE idrepo (
	userId	CHAR(16) NOT NULL,
	password	CHAR(16) NOT NULL,
	firstName	CHAR(16),
	lastName	CHAR(16),
	fullName	CHAR(32),
	PRIMARY KEY (userId)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
