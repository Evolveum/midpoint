CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD fullFlag BOOLEAN;
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp TIMESTAMP;
ALTER TABLE m_task ADD expectedTotal INT8;
ALTER TABLE m_assignment ADD disableReason VARCHAR(255);
ALTER TABLE m_assignment ADD tenantRef_description TEXT;
ALTER TABLE m_assignment ADD tenantRef_filter TEXT;
ALTER TABLE m_assignment ADD tenantRef_relationLocalPart VARCHAR(100);
ALTER TABLE m_assignment ADD tenantRef_relationNamespace VARCHAR(255);
ALTER TABLE m_assignment ADD tenantRef_targetOid VARCHAR(36);
ALTER TABLE m_assignment ADD tenantRef_type INT4;
ALTER TABLE m_focus ADD disableReason VARCHAR(255);
ALTER TABLE m_shadow ADD disableReason VARCHAR(255);
ALTER TABLE m_audit_delta ADD context TEXT;
ALTER TABLE m_audit_delta ADD returns TEXT;
ALTER TABLE m_operation_result ADD context TEXT;
ALTER TABLE m_operation_result ADD returns TEXT;
ALTER TABLE m_object ADD tenantRef_description TEXT;
ALTER TABLE m_object ADD tenantRef_filter TEXT;
ALTER TABLE m_object ADD tenantRef_relationLocalPart VARCHAR(100);
ALTER TABLE m_object ADD tenantRef_relationNamespace VARCHAR(255);
ALTER TABLE m_object ADD tenantRef_targetOid VARCHAR(36);
ALTER TABLE m_object ADD tenantRef_type INT4;
ALTER TABLE m_object ADD name_norm VARCHAR(255);
ALTER TABLE m_object ADD name_orig VARCHAR(255);
ALTER TABLE m_org ADD tenant BOOLEAN;

CREATE TABLE m_report (
    configuration TEXT,
    configurationSchema TEXT,
    dataSource_providerClass VARCHAR(255),
    dataSource_springBean BOOLEAN,
    export INT4,
    field TEXT,
    name_norm VARCHAR(255),
    name_orig VARCHAR(255),
    orientation INT4,
    parent BOOLEAN,
    subreport TEXT,
    template TEXT,
    templateStyle TEXT,
	useHibernateSession BOOLEAN,
	id INT8        NOT NULL,
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

ALTER TABLE m_user ADD jpegPhoto OID;

CREATE INDEX iObjectNameOrig ON m_object (name_orig);

CREATE INDEX iObjectNameNorm ON m_object (name_norm);



 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_connector as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_connector_host as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_generic_object as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_node as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_object_template as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_org as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_report as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_report_output as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_resource as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_role as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_shadow as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_system_configuration as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_task as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_user as x WHERE x.oid = m_object.oid;
 UPDATE m_object SET name_norm = x.name_norm, name_orig = x.name_orig FROM m_value_policy as x WHERE x.oid = m_object.oid;