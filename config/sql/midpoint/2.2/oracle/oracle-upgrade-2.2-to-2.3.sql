CREATE INDEX iParent ON m_task (parent) INITRANS 30;

ALTER TABLE m_sync_situation_description ADD fullFlag NUMBER(1, 0);
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp TIMESTAMP;
ALTER TABLE m_task ADD expectedTotal NUMBER(19, 0);
ALTER TABLE m_assignment ADD disableReason VARCHAR2(255 CHAR);
ALTER TABLE m_assignment ADD tenantRef_description CLOB;
ALTER TABLE m_assignment ADD tenantRef_filter CLOB;
ALTER TABLE m_assignment ADD tenantRef_relationLocalPart VARCHAR(100 CHAR);
ALTER TABLE m_assignment ADD tenantRef_relationNamespace VARCHAR(255 CHAR);
ALTER TABLE m_assignment ADD tenantRef_targetOid VARCHAR(36 CHAR);
ALTER TABLE m_assignment ADD tenantRef_type NUMBER(10, 0);
ALTER TABLE m_focus ADD disableReason VARCHAR2(255 CHAR);
ALTER TABLE m_shadow ADD disableReason VARCHAR2(255 CHAR);
ALTER TABLE m_audit_delta ADD context CLOB;
ALTER TABLE m_audit_delta ADD returns CLOB;
ALTER TABLE m_operation_result ADD context CLOB;
ALTER TABLE m_operation_result ADD returns CLOB;
ALTER TABLE m_object ADD tenantRef_description CLOB;
ALTER TABLE m_object ADD tenantRef_filter CLOB;
ALTER TABLE m_object ADD tenantRef_relationLocalPart VARCHAR(100 CHAR);
ALTER TABLE m_object ADD tenantRef_relationNamespace VARCHAR(255 CHAR);
ALTER TABLE m_object ADD tenantRef_targetOid VARCHAR(36 CHAR);
ALTER TABLE m_object ADD tenantRef_type NUMBER(10,0);
ALTER TABLE m_object ADD name_norm VARCHAR(255 CHAR);
ALTER TABLE m_object ADD name_orig VARCHAR(255 CHAR);
ALTER TABLE m_org ADD tenant NUMBER(1, 0);
ALTER TABLE m_system_configuration ADD objectTemplate CLOB;


CREATE TABLE m_report (
    configuration CLOB,
    configurationSchema CLOB,
    dataSource_providerClass VARCHAR2(255 CHAR),
    dataSource_springBean NUMBER(1,0),
    export NUMBER(10,0),
    field CLOB,
    name_norm VARCHAR2(255 CHAR),
    name_orig VARCHAR2(255 CHAR),
    orientation NUMBER(10,0),
    parent NUMBER(1,0),
    subreport CLOB,
    template CLOB,
    templateStyle CLOB,
    useHibernateSession NUMBER(1,0),
    id number(19,0) NOT NULL,
    oid VARCHAR2(36 CHAR) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
) INITRANS 30;

CREATE INDEX iReportParent ON m_report (parent) INITRANS 30;

CREATE INDEX iReportName ON m_report (name_orig) INITRANS 30;

ALTER TABLE m_report 
    ADD CONSTRAINT fk_report 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object;

CREATE INDEX iAncestorDepth ON m_org_closure (ancestor_id, ancestor_oid, depthValue) INITRANS 30;

CREATE TABLE m_report_output (
    name_norm VARCHAR2(255 CHAR),
    name_orig VARCHAR2(255 CHAR),
    reportFilePath VARCHAR2(255 CHAR),
    reportRef_description CLOB,
    reportRef_filter CLOB,
    reportRef_relationLocalPart VARCHAR2(100 CHAR),
    reportRef_relationNamespace VARCHAR2(255 CHAR),
    reportRef_targetOid VARCHAR2(36 CHAR),
    reportRef_type NUMBER(10,0),
    id NUMBER(19,0) NOT NULL,
    oid VARCHAR2(36 CHAR) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
) INITRANS 30;

CREATE INDEX iReportOutputName ON m_report_output (name_orig) INITRANS 30;

ALTER TABLE m_report_output 
    ADD CONSTRAINT fk_reportoutput 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object;

ALTER TABLE m_assignment ADD orderValue NUMBER(10,0);

ALTER TABLE m_user ADD jpegPhoto BLOB;

CREATE INDEX iObjectNameOrig ON m_object (name_orig) INITRANS 30;

CREATE INDEX iObjectNameNorm ON m_object (name_norm) INITRANS 30;


--UPDATE NAMES IN m_object
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

 ALTER TABLE m_authorization ADD objectSpecification CLOB INITRANS 30;
 
 
 CREATE TABLE m_security_policy (
    authentication CLOB,
    credentials CLOB,
    name_norm VARCHAR2(255 CHAR),
    name_orig VARCHAR2(255 CHAR),
	id NUMBER(19,0) NOT NULL,
    oid VARCHAR2(36 CHAR) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
) INITRANS 30;


CREATE INDEX iSecurityPolicyName ON m_security_policy (name_orig) INITRANS 30;

ALTER TABLE m_security_policy 
    ADD CONSTRAINT fk_security_policy 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object;

ALTER TABLE m_object_template ADD iteration CLOB;

ALTER TABLE m_focus ADD iteration NUMBER(10,0);
ALTER TABLE m_focus ADD iterationToken VARCHAR(255 CHAR);

