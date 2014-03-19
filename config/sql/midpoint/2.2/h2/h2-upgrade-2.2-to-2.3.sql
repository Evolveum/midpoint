CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD fullFlag BOOLEAN;
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp TIMESTAMP;
ALTER TABLE m_task ADD expectedTotal BIGINT;
ALTER TABLE m_assignment ADD disableReason VARCHAR(255);
ALTER TABLE m_assignment ADD tenantRef_description CLOB;
ALTER TABLE m_assignment ADD tenantRef_filter CLOB;
ALTER TABLE m_assignment ADD tenantRef_relationLocalPart VARCHAR(100);
ALTER TABLE m_assignment ADD tenantRef_relationNamespace VARCHAR(255);
ALTER TABLE m_assignment ADD tenantRef_targetOid VARCHAR(36);
ALTER TABLE m_assignment ADD tenantRef_type INTEGER;
ALTER TABLE m_focus ADD disableReason VARCHAR(255);
ALTER TABLE m_shadow ADD disableReason VARCHAR(255);
ALTER TABLE m_audit_delta ADD context CLOB;
ALTER TABLE m_audit_delta ADD returns CLOB;
ALTER TABLE m_operation_result ADD context CLOB;
ALTER TABLE m_operation_result ADD returns CLOB;
ALTER TABLE m_object ADD tenantRef_description CLOB;
ALTER TABLE m_object ADD tenantRef_filter CLOB;
ALTER TABLE m_object ADD tenantRef_relationLocalPart VARCHAR(100);
ALTER TABLE m_object ADD tenantRef_relationNamespace VARCHAR(255);
ALTER TABLE m_object ADD tenantRef_targetOid VARCHAR(36);
ALTER TABLE m_object ADD tenantRef_type INTEGER;
ALTER TABLE m_object ADD name_norm VARCHAR(255);
ALTER TABLE m_object ADD name_orig VARCHAR(255);
ALTER TABLE m_org ADD tenant BOOLEAN;
ALTER TABLE m_system_configuration ADD objectTemplate CLOB;

CREATE TABLE m_report (
    configuration CLOB,
    configurationSchema CLOB,
    dataSource_providerClass VARCHAR(255),
    dataSource_springBean BOOLEAN,
    export INTEGER,
    field CLOB,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    orientation INTEGER,
    parent BOOLEAN,
    subreport CLOB,
    template CLOB,
    templateStyle CLOB,
    useHibernateSession BOOLEAN,
	id BIGINT NOT NULL,
	oid VARCHAR(36) NOT NULL,
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
	reportRef_description CLOB,
	reportRef_filter CLOB,
	reportRef_relationLocalPart VARCHAR(100),
	reportRef_relationNamespace VARCHAR(255),
	reportRef_targetOid VARCHAR(36),
	reportRef_type INTEGER,
	id BIGINT NOT NULL,
    oid VARCHAR(36) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
);

CREATE INDEX iReportOutputName ON m_report_output (name_orig);

ALTER TABLE m_report_output 
    ADD CONSTRAINT fk_reportoutput 
	FOREIGN KEY (id, oid) 
    REFERENCES m_object;


ALTER TABLE m_assignment ADD orderValue INTEGER;

ALTER TABLE m_user ADD jpegPhoto BLOB;

CREATE INDEX iObjectNameOrig ON m_object (name_orig);

CREATE INDEX iObjectNameNorm ON m_object (name_norm);



 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_connector x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_connector x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_connector_host x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_connector_host x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_generic_object x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_generic_object x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_node x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_node x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_object_template x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_object_template x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_org x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_org x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_report x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_report x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_report_output x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_report_output x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_resource x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_resource x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_role x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_role x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_shadow x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_shadow x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_system_configuration x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_system_configuration x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_task x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_task x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_user x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_user x WHERE x.oid = o.oid);
 UPDATE m_object o SET (o.name_norm, o.name_orig) = (SELECT x.name_norm, x.name_orig FROM m_value_policy x WHERE x.oid = o.oid) 
 WHERE EXISTS (SELECT x.name_norm, x.name_orig FROM m_value_policy x WHERE x.oid = o.oid);
 
 ALTER TABLE m_authorization ADD objectSpecification CLOB;
