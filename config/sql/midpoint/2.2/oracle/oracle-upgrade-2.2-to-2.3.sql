CREATE INDEX iParent ON m_task (parent) INITRANS 30;

ALTER TABLE m_sync_situation_description ADD fullFlag NUMBER(1, 0);
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp TIMESTAMP;
ALTER TABLE m_task ADD expectedTotal NUMBER(19, 0);
ALTER TABLE m_assignment ADD disableReason VARCHAR2(255 CHAR);
ALTER TABLE m_focus ADD disableReason VARCHAR2(255 CHAR);
ALTER TABLE m_shadow ADD disableReason VARCHAR2(255 CHAR);
ALTER TABLE m_audit_delta ADD context CLOB;
ALTER TABLE m_audit_delta ADD returns CLOB;
ALTER TABLE m_operation_result ADD context CLOB;
ALTER TABLE m_operation_result ADD returns CLOB;

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