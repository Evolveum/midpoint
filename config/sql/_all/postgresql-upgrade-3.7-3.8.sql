CREATE SEQUENCE m_audit_event_id_seq;
ALTER TABLE m_audit_event ALTER COLUMN id SET NOT NULL;
ALTER TABLE m_audit_event ALTER COLUMN id SET DEFAULT nextval('m_audit_event_id_seq');
ALTER SEQUENCE m_audit_event_id_seq OWNED BY m_audit_event.id;

CREATE SEQUENCE m_audit_prop_value_id_seq;
ALTER TABLE m_audit_prop_value ALTER COLUMN id SET NOT NULL;
ALTER TABLE m_audit_prop_value ALTER COLUMN id SET DEFAULT nextval('m_audit_prop_value_id_seq');
ALTER SEQUENCE m_audit_prop_value_id_seq OWNED BY m_audit_prop_value.id;

CREATE SEQUENCE m_audit_ref_value_id_seq;
ALTER TABLE m_audit_ref_value ALTER COLUMN id SET NOT NULL;
ALTER TABLE m_audit_ref_value ALTER COLUMN id SET DEFAULT nextval('m_audit_ref_value_id_seq');
ALTER SEQUENCE m_audit_ref_value_id_seq OWNED BY m_audit_ref_value.id;

SELECT setval('m_audit_event_id_seq', (SELECT max(id) + 1 FROM m_audit_event), FALSE);
SELECT setval('m_audit_prop_value_id_seq', (SELECT max(id) + 1 FROM m_audit_prop_value), FALSE);
SELECT setval('m_audit_ref_value_id_seq', (SELECT max(id) + 1 FROM m_audit_ref_value), FALSE);

