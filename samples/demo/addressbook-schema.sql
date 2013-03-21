CREATE TABLE people (
	username		VARCHAR(64) PRIMARY KEY,
	first_name		VARCHAR(100),
	last_name		VARCHAR(100) NOT NULL,
	tel_number		VARCHAR(32),
	fax_number		VARCHAR(32),
	office_id		VARCHAR(32),
	floor			INTEGER,
	street_address		VARCHAR(100),
	city			VARCHAR(100),
	country			VARCHAR(100),
	postal_code		VARCHAR(16),
	validity		BOOLEAN,
	created			TIMESTAMP,
	modified		TIMESTAMP,
	password		VARCHAR(64)
);
