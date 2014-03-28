CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD fullFlag BIT;
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp DATETIME(6);
ALTER TABLE m_task ADD expectedTotal BIGINT;
ALTER TABLE m_assignment ADD disableReason VARCHAR(255);
ALTER TABLE m_assignment ADD tenantRef_description LONGTEXT;
ALTER TABLE m_assignment ADD tenantRef_filter LONGTEXT;
ALTER TABLE m_assignment ADD tenantRef_relationLocalPart VARCHAR(100);
ALTER TABLE m_assignment ADD tenantRef_relationNamespace VARCHAR(255);
ALTER TABLE m_assignment ADD tenantRef_targetOid VARCHAR(36);
ALTER TABLE m_assignment ADD tenantRef_type INTEGER;
ALTER TABLE m_focus ADD disableReason VARCHAR(255);
ALTER TABLE m_shadow ADD disableReason VARCHAR(255);
ALTER TABLE m_audit_delta ADD context LONGTEXT;
ALTER TABLE m_audit_delta ADD returns LONGTEXT;
ALTER TABLE m_operation_result ADD context LONGTEXT;
ALTER TABLE m_operation_result ADD returns LONGTEXT;
ALTER TABLE m_object ADD tenantRef_description LONGTEXT;
ALTER TABLE m_object ADD tenantRef_filter LONGTEXT;
ALTER TABLE m_object ADD tenantRef_relationLocalPart VARCHAR(100);
ALTER TABLE m_object ADD tenantRef_relationNamespace VARCHAR(255);
ALTER TABLE m_object ADD tenantRef_targetOid VARCHAR(36);
ALTER TABLE m_object ADD tenantRef_type INTEGER;
ALTER TABLE m_object ADD name_norm VARCHAR(255);
ALTER TABLE m_object ADD name_orig VARCHAR(255);
ALTER TABLE m_org ADD tenant BIT;
ALTER TABLE m_system_configuration ADD objectTemplate LONGTEXT;

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

CREATE INDEX iObjectNameOrig ON m_object (name_orig);

CREATE INDEX iObjectNameNorm ON m_object (name_norm);


 UPDATE m_object as o, m_connector as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_connector_host as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_generic_object as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_node as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_object_template as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_org as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_report as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_report_output as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_resource as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_role as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_shadow as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_system_configuration as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_task as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_user as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 UPDATE m_object as o, m_value_policy as x SET o.name_norm = x.name_norm, o.name_orig = x.name_orig  WHERE x.oid = o.oid;
 
 ALTER TABLE m_authorization ADD objectSpecification LONGTEXT;
  
 CREATE TABLE m_security_policy (
    authentication LONGTEXT,
    credentials LONGTEXT,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
	id BIGINT NOT NULL,
    oid VARCHAR(36) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
);

CREATE INDEX iSecurityPolicyName ON m_security_policy (name_orig);

ALTER TABLE m_security_policy 
	ADD INDEX fk_security_policy (id, oid), 
    ADD CONSTRAINT fk_security_policy 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object (id, oid);
	
ALTER TABLE m_object_template ADD iteration LONGTEXT;

ALTER TABLE m_focua ADD iteration INTEGER;
ALTER TABLE m_focua ADD iterationToken VARCHAR(255);

