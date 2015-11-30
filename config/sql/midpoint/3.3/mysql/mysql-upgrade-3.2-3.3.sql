CREATE TABLE m_sequence (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE = InnoDB;

ALTER TABLE m_sequence
ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);

ALTER TABLE m_sequence
ADD CONSTRAINT fk_sequence
FOREIGN KEY (oid)
REFERENCES m_object (oid);

RENAME TABLE m_user_photo TO m_focus_photo;

ALTER TABLE m_focus ADD hasPhoto BIT NOT NULL DEFAULT 0
AFTER validityStatus;

UPDATE m_focus
SET hasPhoto = 0;
UPDATE m_focus
SET hasPhoto = (SELECT hasPhoto
                FROM m_user
                WHERE m_user.oid = m_focus.oid)
WHERE m_focus.oid IN (SELECT oid
                      FROM m_user);

ALTER TABLE m_user DROP COLUMN hasPhoto;

ALTER TABLE m_focus_photo
DROP FOREIGN KEY fk_user_photo;

ALTER TABLE m_focus_photo
ADD CONSTRAINT fk_focus_photo
FOREIGN KEY (owner_oid)
REFERENCES m_focus (oid);

ALTER TABLE m_assignment ADD orgRef_relation VARCHAR(157)
AFTER orderValue,
ADD orgRef_targetOid VARCHAR(36)
AFTER orgRef_relation,
ADD orgRef_type INTEGER
AFTER orgRef_targetOid,
ADD resourceRef_relation VARCHAR(157)
AFTER orgRef_type,
ADD resourceRef_targetOid VARCHAR(36)
AFTER resourceRef_relation,
ADD resourceRef_type INTEGER
AFTER resourceRef_targetOid;

CREATE INDEX iTargetRefTargetOid ON m_assignment (targetRef_targetOid);
CREATE INDEX iTenantRefTargetOid ON m_assignment (tenantRef_targetOid);
CREATE INDEX iOrgRefTargetOid ON m_assignment (orgRef_targetOid);
CREATE INDEX iResourceRefTargetOid ON m_assignment (resourceRef_targetOid);

CREATE INDEX iTimestampValue ON m_audit_event (timestampValue);

ALTER TABLE m_audit_delta
ADD objectName_norm VARCHAR(255),
ADD objectName_orig VARCHAR(255),
ADD resourceName_norm VARCHAR(255),
ADD resourceName_orig VARCHAR(255),
ADD resourceOid VARCHAR(36);

