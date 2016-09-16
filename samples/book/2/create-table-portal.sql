
CREATE TABLE portalusers (
  login             VARCHAR(16) NOT NULL,
  ldapDn			VARCHAR(128),
  fullName           VARCHAR(32),
  disabled           BOOLEAN,
  PRIMARY KEY (login)
);
