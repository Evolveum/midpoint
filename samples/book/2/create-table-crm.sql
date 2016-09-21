
CREATE TABLE crmusers (
  userId             VARCHAR(16) NOT NULL,
  password           VARCHAR(16) NOT NULL,
  firstName	         VARCHAR(16),
  lastName	         VARCHAR(16),
  fullName           VARCHAR(32),
  description        VARCHAR(256),
  accessLevel        VARCHAR(256),
  disabled           BOOLEAN,
  PRIMARY KEY (userId)
);
