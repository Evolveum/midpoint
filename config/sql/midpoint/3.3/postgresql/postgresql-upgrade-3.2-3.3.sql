CREATE TABLE m_sequence (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_sequence
ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);

ALTER TABLE m_sequence
ADD CONSTRAINT fk_sequence
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_user_photo RENAME TO m_focus_photo;

ALTER TABLE m_focus ADD hasPhoto BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE m_focus
SET hasPhoto = FALSE;
UPDATE m_focus
SET hasPhoto = (SELECT hasPhoto
                FROM m_user
                WHERE m_user.oid = m_focus.oid)
WHERE m_focus.oid IN (SELECT oid
                      FROM m_user);

ALTER TABLE m_focus_photo
DROP CONSTRAINT m_user_photo_pkey;

ALTER TABLE m_focus_photo
ADD CONSTRAINT m_focus_photo_pkey PRIMARY KEY (owner_oid);

ALTER TABLE m_focus_photo
DROP CONSTRAINT fk_user_photo;

ALTER TABLE m_focus_photo
ADD CONSTRAINT fk_focus_photo
FOREIGN KEY (owner_oid)
REFERENCES m_focus;

ALTER TABLE m_user DROP COLUMN hasPhoto;

ALTER TABLE m_assignment
ADD orgRef_relation VARCHAR(157),
ADD orgRef_targetOid VARCHAR(36),
ADD orgRef_type INT4,
ADD resourceRef_relation VARCHAR(157),
ADD resourceRef_targetOid VARCHAR(36),
ADD resourceRef_type INT4;

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

