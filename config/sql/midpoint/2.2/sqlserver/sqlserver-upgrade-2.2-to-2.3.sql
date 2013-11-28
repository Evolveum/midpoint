CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_sync_situation_description ADD fullFlag BIT;
ALTER TABLE m_shadow ADD fullSynchronizationTimestamp DATETIME2;
ALTER TABLE m_task ADD expectedTotal BIGINT;
ALTER TABLE m_assignment ADD disableReason NVARCHAR(255);
ALTER TABLE m_focus ADD disableReason NVARCHAR(255);
ALTER TABLE m_shadow ADD disableReason NVARCHAR(255);

CREATE TABLE m_report (
    name_norm NVARCHAR(255),
    name_orig NVARCHAR(255),
    class_namespace NVARCHAR(255),
    class_localPart NVARCHAR(100),
    query NVARCHAR(MAX),
    reportExport INT,
    reportFields NVARCHAR(MAX),
    reportOrientation INT,
    reportParameters NVARCHAR(MAX),
    reportTemplateJRXML NVARCHAR(MAX),
    reportTemplateStyleJRTX NVARCHAR(MAX),
    id BIGINT NOT NULL,
    oid NVARCHAR(36) NOT NULL,
    PRIMARY KEY (id, oid),
    UNIQUE (name_norm)
);


CREATE INDEX iReportName ON m_report (name_orig);

ALTER TABLE m_report 
    ADD CONSTRAINT fk_report 
    FOREIGN KEY (id, oid) 
    REFERENCES m_object;
