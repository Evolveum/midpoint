--- todo figure out what to do with audit tables (changed id column, now with identity)

drop table m_audit_ref_value;
drop table m_audit_prop_value;
drop table m_audit_item;
drop table m_audit_delta;
drop table m_audit_event;

create table m_audit_delta (checksum nvarchar(32) collate database_default not null, record_id bigint not null, delta nvarchar(MAX), deltaOid nvarchar(36) collate database_default, deltaType int, fullResult nvarchar(MAX), objectName_norm nvarchar(255) collate database_default, objectName_orig nvarchar(255) collate database_default, resourceName_norm nvarchar(255) collate database_default, resourceName_orig nvarchar(255) collate database_default, resourceOid nvarchar(36) collate database_default, status int, primary key (checksum, record_id));
create table m_audit_event (id bigint identity not null, attorneyName nvarchar(255) collate database_default, attorneyOid nvarchar(36) collate database_default, channel nvarchar(255) collate database_default, eventIdentifier nvarchar(255) collate database_default, eventStage int, eventType int, hostIdentifier nvarchar(255) collate database_default, initiatorName nvarchar(255) collate database_default, initiatorOid nvarchar(36) collate database_default, initiatorType int, message nvarchar(1024) collate database_default, nodeIdentifier nvarchar(255) collate database_default, outcome int, parameter nvarchar(255) collate database_default, remoteHostAddress nvarchar(255) collate database_default, result nvarchar(255) collate database_default, sessionIdentifier nvarchar(255) collate database_default, targetName nvarchar(255) collate database_default, targetOid nvarchar(36) collate database_default, targetOwnerName nvarchar(255) collate database_default, targetOwnerOid nvarchar(36) collate database_default, targetType int, taskIdentifier nvarchar(255) collate database_default, taskOID nvarchar(255) collate database_default, timestampValue datetime2, primary key (id));
create table m_audit_item (changedItemPath nvarchar(900) collate database_default not null, record_id bigint not null, primary key (changedItemPath, record_id));
create table m_audit_prop_value (id bigint identity not null, name nvarchar(255) collate database_default, record_id bigint, value nvarchar(1024) collate database_default, primary key (id));
create table m_audit_ref_value (id bigint identity not null, name nvarchar(255) collate database_default, oid nvarchar(255) collate database_default, record_id bigint, targetName_norm nvarchar(255) collate database_default, targetName_orig nvarchar(255) collate database_default, type nvarchar(255) collate database_default, primary key (id));

create index iTimestampValue on m_audit_event (timestampValue);
create index iChangedItemPath on m_audit_item (changedItemPath);
create index iAuditPropValRecordId on m_audit_prop_value (record_id);
create index iAuditRefValRecordId on m_audit_ref_value (record_id);

alter table m_audit_delta add constraint fk_audit_delta foreign key (record_id) references m_audit_event;
alter table m_audit_item add constraint fk_audit_item foreign key (record_id) references m_audit_event;
alter table m_audit_prop_value add constraint fk_audit_prop_value foreign key (record_id) references m_audit_event;
alter table m_audit_ref_value add constraint fk_audit_ref_value foreign key (record_id) references m_audit_event;
