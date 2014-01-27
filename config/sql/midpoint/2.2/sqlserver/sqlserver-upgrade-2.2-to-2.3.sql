CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD fullFlag BIT;
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp DATETIME2;
ALTER TABLE m_task ADD expectedTotal BIGINT;
ALTER TABLE m_assignment ADD disableReason NVARCHAR(255);
ALTER TABLE m_focus ADD disableReason NVARCHAR(255);
ALTER TABLE m_shadow ADD disableReason NVARCHAR(255);
ALTER TABLE m_audit_delta ADD context NVARCHAR(MAX);
ALTER TABLE m_audit_delta ADD returns NVARCHAR(MAX);
ALTER TABLE m_operation_result ADD context NVARCHAR(MAX);
ALTER TABLE m_operation_result ADD returns NVARCHAR(MAX);

CREATE TABLE m_report (
  configuration            NVARCHAR(MAX),
  configurationSchema      NVARCHAR(MAX),
  dataSource_providerClass NVARCHAR(255),
  dataSource_springBean    BIT          NOT NULL,
  name_norm                NVARCHAR(255),
  name_orig                NVARCHAR(255),
  parent                   BIT,
  reportExport             INT,
  reportFields             NVARCHAR(MAX),
  reportOrientation        INT,
  reportTemplate           NVARCHAR(MAX),
  reportTemplateStyle      NVARCHAR(MAX),
  subReport                NVARCHAR(MAX),
  useHibernateSession      BIT,
  id                       BIGINT       NOT NULL,
  oid                      NVARCHAR(36) NOT NULL,
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
    name_norm NVARCHAR(255),
    name_orig NVARCHAR(255),
    reportFilePath NVARCHAR(255),
    reportRef_description NVARCHAR(MAX),
    reportRef_filter NVARCHAR(MAX),
    reportRef_relationLocalPart NVARCHAR(100),
    reportRef_relationNamespace NVARCHAR(255),
    reportRef_targetOid NVARCHAR(36),
    reportRef_type INT,
    id BIGINT NOT NULL,
    oid NVARCHAR(36) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
);
	
CREATE INDEX iReportOutputName ON m_report_output (name_orig);

ALTER TABLE m_report_output 
    ADD CONSTRAINT fk_reportoutput 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object;

ALTER TABLE m_assignment ADD orderValue INT;