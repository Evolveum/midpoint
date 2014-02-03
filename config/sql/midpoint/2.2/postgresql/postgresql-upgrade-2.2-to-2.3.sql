CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD COLUMN fullFlag BOOLEAN;
ALTER TABLE m_shadow ADD COLUMN fullSynchronizationTimestamp TIMESTAMP;
ALTER TABLE m_task ADD COLUMN expectedTotal INT8;
ALTER TABLE m_assignment ADD disableReason VARCHAR(255);
ALTER TABLE m_focus ADD disableReason VARCHAR(255);
ALTER TABLE m_shadow ADD disableReason VARCHAR(255);
ALTER TABLE m_audit_delta ADD context TEXT;
ALTER TABLE m_audit_delta ADD returns TEXT;
ALTER TABLE m_operation_result ADD context TEXT;
ALTER TABLE m_operation_result ADD returns TEXT;

CREATE TABLE m_report (
  configuration            TEXT,
  configurationSchema      TEXT,
  dataSource_providerClass VARCHAR(255),
  dataSource_springBean    BOOLEAN,
  name_norm                VARCHAR(255),
  name_orig                VARCHAR(255),
  parent                   BOOLEAN,
  reportExport             INT4,
  reportFields             TEXT,
  reportOrientation        INT4,
  reportTemplate           TEXT,
  reportTemplateStyle      TEXT,
  subReport                TEXT,
  useHibernateSession      BOOLEAN,
  id                       INT8        NOT NULL,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (id, oid),
  UNIQUE (name_norm)
);

CREATE INDEX iReportParent ON m_report (parent);

CREATE INDEX iReportName ON m_report (name_orig);

ALTER TABLE m_report 
    ADD CONSTRAINT fk_report 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object;

CREATE INDEX iAncestorDepth ON m_org_closure (ancestor_id, ancestor_oid, depthValue);

CREATE TABLE m_report_output (
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    reportFilePath VARCHAR(255),
    reportRef_description TEXT,
    reportRef_filter TEXT,
    reportRef_relationLocalPart VARCHAR(100),
    reportRef_relationNamespace VARCHAR(255),
    reportRef_targetOid VARCHAR(36),
    reportRef_type INT4,
    id INT8 NOT NULL,
    oid VARCHAR(36) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
);
	
CREATE INDEX iReportOutputName ON m_report_output (name_orig);

ALTER TABLE m_report_output 
    ADD CONSTRAINT fk_reportoutput 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object;

ALTER TABLE m_assignment ADD orderValue INT4;