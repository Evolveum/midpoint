CREATE TABLE m_sequence (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_sequence
ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);

ALTER TABLE m_sequence
ADD CONSTRAINT fk_sequence
FOREIGN KEY (oid)
REFERENCES m_object;

EXEC sp_rename m_user_photo, m_focus_photo;

ALTER TABLE m_focus ADD hasPhoto BIT NOT NULL CONSTRAINT default_constraint DEFAULT 0;

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
DROP CONSTRAINT fk_user_photo;

ALTER TABLE m_focus_photo
ADD CONSTRAINT fk_focus_photo
FOREIGN KEY (owner_oid)
REFERENCES m_focus;

ALTER TABLE m_assignment ADD
orgRef_relation NVARCHAR(157) COLLATE database_default,
orgRef_targetOid NVARCHAR(36) COLLATE database_default,
orgRef_type INT,
resourceRef_relation NVARCHAR(157) COLLATE database_default,
resourceRef_targetOid NVARCHAR(36) COLLATE database_default,
resourceRef_type INT;

CREATE INDEX iTargetRefTargetOid ON m_assignment (targetRef_targetOid);
CREATE INDEX iTenantRefTargetOid ON m_assignment (tenantRef_targetOid);
CREATE INDEX iOrgRefTargetOid ON m_assignment (orgRef_targetOid);
CREATE INDEX iResourceRefTargetOid ON m_assignment (resourceRef_targetOid);

CREATE INDEX iTimestampValue ON m_audit_event (timestampValue);

ALTER TABLE m_audit_delta ADD
objectName_norm NVARCHAR(255) COLLATE database_default,
objectName_orig NVARCHAR(255) COLLATE database_default,
resourceName_norm NVARCHAR(255) COLLATE database_default,
resourceName_orig NVARCHAR(255) COLLATE database_default,
resourceOid NVARCHAR(36) COLLATE database_default;

