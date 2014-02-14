CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD fullFlag BIT;
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp DATETIME(6);
ALTER TABLE m_task ADD expectedTotal BIGINT;
ALTER TABLE m_assignment ADD disableReason VARCHAR(255);
ALTER TABLE m_focus ADD disableReason VARCHAR(255);
ALTER TABLE m_shadow ADD disableReason VARCHAR(255);
ALTER TABLE m_audit_delta ADD context LONGTEXT;
ALTER TABLE m_audit_delta ADD returns LONGTEXT;
ALTER TABLE m_operation_result ADD context LONGTEXT;
ALTER TABLE m_operation_result ADD returns LONGTEXT;

CREATE TABLE m_report (
	configuration LONGTEXT,
    configurationSchema LONGTEXT,
    dataSource_providerClass VARCHAR(255),
    dataSource_springBean BIT,
    export INTEGER,
    field LONGTEXT,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    orientation INTEGER,
    parent BIT,
    subreport LONGTEXT,
    template LONGTEXT,
    templateStyle LONGTEXT,
    useHibernateSession BIT,
	id BIGINT NOT NULL,
	oid VARCHAR(36) NOT NULL,
	PRIMARY KEY (id, oid),
	UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE INDEX iReportParent ON m_report (parent);
	
CREATE INDEX iReportName ON m_report (name_orig);

ALTER TABLE m_report
	ADD INDEX fk_report (id, oid), 
    ADD CONSTRAINT fk_report 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object (id, oid);

CREATE INDEX iAncestorDepth ON m_org_closure (ancestor_id, ancestor_oid, depthValue);

CREATE TABLE m_report_output (
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    reportFilePath VARCHAR(255),
    reportRef_description longtext,
    reportRef_filter longtext,
    reportRef_relationLocalPart VARCHAR(100),
    reportRef_relationNamespace VARCHAR(255),
    reportRef_targetOid VARCHAR(36),
    reportRef_type INTEGER,
    id BIGINT NOT NULL,
    oid VARCHAR(36) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;
	
	
CREATE INDEX iReportOutputName ON m_report_output (name_orig);

ALTER TABLE m_report_output 
    ADD INDEX fk_reportoutput (id, oid), 
    ADD CONSTRAINT fk_reportoutput 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object (id, oid);

ALTER TABLE m_assignment ADD orderValue INTEGER;

ALTER TABLE m_user ADD jpegPhoto LONGBLOB;