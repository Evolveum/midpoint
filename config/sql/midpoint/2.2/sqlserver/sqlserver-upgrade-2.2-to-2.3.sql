CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD fullFlag BIT;
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp DATETIME2;
ALTER TABLE m_task ADD expectedTotal BIGINT;
ALTER TABLE m_assignment ADD disableReason NVARCHAR(255);
ALTER TABLE m_assignment ADD tenantRef_description NVARCHAR(MAX);
ALTER TABLE m_assignment ADD tenantRef_filter NVARCHAR(MAX);
ALTER TABLE m_assignment ADD tenantRef_relationLocalPart NVARCHAR(100);
ALTER TABLE m_assignment ADD tenantRef_relationNamespace NVARCHAR(255);
ALTER TABLE m_assignment ADD tenantRef_targetOid NVARCHAR(36);
ALTER TABLE m_assignment ADD tenantRef_type INT;
ALTER TABLE m_focus ADD disableReason NVARCHAR(255);
ALTER TABLE m_shadow ADD disableReason NVARCHAR(255);
ALTER TABLE m_audit_delta ADD context NVARCHAR(MAX);
ALTER TABLE m_audit_delta ADD returns NVARCHAR(MAX);
ALTER TABLE m_operation_result ADD context NVARCHAR(MAX);
ALTER TABLE m_operation_result ADD returns NVARCHAR(MAX);
ALTER TABLE m_object ADD tenantRef_description NVARCHAR(MAX);
ALTER TABLE m_object ADD tenantRef_filter NVARCHAR(MAX);
ALTER TABLE m_object ADD tenantRef_relationLocalPart NVARCHAR(100);
ALTER TABLE m_object ADD tenantRef_relationNamespace NVARCHAR(255);
ALTER TABLE m_object ADD tenantRef_targetOid NVARCHAR(36);
ALTER TABLE m_object ADD tenantRef_type INT;
ALTER TABLE m_object ADD name_norm NVARCHAR(255);
ALTER TABLE m_object ADD name_orig NVARCHAR(255);
ALTER TABLE m_org ADD tenant BIT;


CREATE TABLE m_report (
	configuration NVARCHAR(MAX),
    configurationSchema NVARCHAR(MAX),
    dataSource_providerClass NVARCHAR(255),
    dataSource_springBean BIT,
    export INT,
    name_norm NVARCHAR(255),
    name_orig NVARCHAR(255),
    orientation INT,
    parent BIT,
    field NVARCHAR(MAX),
    subreport NVARCHAR(MAX),
    template NVARCHAR(MAX),
    templateStyle NVARCHAR(MAX),
    useHibernateSession BIT,
	id BIGINT NOT NULL,
	oid NVARCHAR(36) NOT NULL,
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

ALTER TABLE m_user ADD jpegPhoto VARBINARY(MAX);

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