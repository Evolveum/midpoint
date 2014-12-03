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
	timestamp	TIMESTAMP
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

