CREATE TABLE m_sequence (
  name_norm VARCHAR2(255 CHAR),
  name_orig VARCHAR2(255 CHAR),
  oid       VARCHAR2(36 CHAR) NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE m_sequence
ADD CONSTRAINT uc_sequence_name UNIQUE (name_norm);

ALTER TABLE m_sequence
ADD CONSTRAINT fk_sequence
FOREIGN KEY (oid)
REFERENCES m_object;


ALTER TABLE m_user_photo
DROP CONSTRAINT fk_user_photo;

RENAME m_user_photo TO m_focus_photo;

ALTER TABLE m_focus_photo
ADD CONSTRAINT fk_focus_photo
FOREIGN KEY (owner_oid)
REFERENCES m_focus;

ALTER TABLE m_focus ADD hasPhoto NUMBER(1, 0) DEFAULT 0 NOT NULL;
UPDATE m_focus
SET hasPhoto = 0;
UPDATE m_focus
SET hasPhoto = (SELECT hasPhoto
                FROM m_user
                WHERE m_user.oid = m_focus.oid)
WHERE m_focus.oid IN (SELECT oid
                      FROM m_user);

ALTER TABLE m_user DROP COLUMN hasPhoto;


ALTER TABLE m_assignment ADD (
orgRef_relation VARCHAR2(157 CHAR),
orgRef_targetOid VARCHAR2(36 CHAR),
orgRef_type NUMBER(10, 0),
resourceRef_relation VARCHAR2(157 CHAR),
resourceRef_targetOid VARCHAR2(36 CHAR),
resourceRef_type NUMBER(10, 0));

CREATE INDEX iTargetRefTargetOid ON m_assignment (targetRef_targetOid);
CREATE INDEX iTenantRefTargetOid ON m_assignment (tenantRef_targetOid);
CREATE INDEX iOrgRefTargetOid ON m_assignment (orgRef_targetOid);
CREATE INDEX iResourceRefTargetOid ON m_assignment (resourceRef_targetOid);

CREATE INDEX iTimestampValue ON m_audit_event (timestampValue);

ALTER TABLE m_audit_delta ADD (
objectName_norm VARCHAR2(255 CHAR),
objectName_orig VARCHAR2(255 CHAR),
resourceName_norm VARCHAR2(255 CHAR),
resourceName_orig VARCHAR2(255 CHAR),
resourceOid VARCHAR2(36 CHAR));
