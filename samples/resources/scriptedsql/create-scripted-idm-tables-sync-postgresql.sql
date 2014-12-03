CREATE TABLE Users (
	id		SERIAL PRIMARY KEY,
	login		VARCHAR(32) NOT NULL,
	firstname	VARCHAR(255),
	lastname	VARCHAR(255),
	fullname	VARCHAR(255),
	email		VARCHAR(255),
	organization	VARCHAR(255),
	password	VARCHAR(255),
	disabled	BOOLEAN DEFAULT false,
	timestamp	TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE Groups (
	id		SERIAL PRIMARY KEY,
	name		VARCHAR(255) NOT NULL,
	description	VARCHAR(255)
);

CREATE TABLE Organizations (
	id		SERIAL PRIMARY KEY,
	name		VARCHAR(255) NOT NULL,
	description	VARCHAR(255)
);

CREATE OR REPLACE FUNCTION update_timestamp_column()	
RETURNS TRIGGER AS $$
BEGIN
    NEW.timestamp = now();
    RETURN NEW;	
END;
$$ language 'plpgsql';

CREATE TRIGGER update_account_timestamp BEFORE UPDATE ON Users FOR EACH ROW EXECUTE PROCEDURE  update_timestamp_column();
