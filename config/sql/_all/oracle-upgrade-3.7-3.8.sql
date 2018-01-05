--- todo figure out what to do with audit tables (changed id column, now with identity)

drop table m_audit_ref_value;
drop table m_audit_prop_value;
drop table m_audit_item;
drop table m_audit_delta;
drop table m_audit_event;

create table m_audit_delta (checksum varchar2(32 char) not null, record_id number(19,0) not null, delta clob, deltaOid varchar2(36 char), deltaType number(10,0), fullResult clob, objectName_norm varchar2(255 char), objectName_orig varchar2(255 char), resourceName_norm varchar2(255 char), resourceName_orig varchar2(255 char), resourceOid varchar2(36 char), status number(10,0), primary key (checksum, record_id)) initrans 30;
create table m_audit_event (id number(19,0) generated as identity, attorneyName varchar2(255 char), attorneyOid varchar2(36 char), channel varchar2(255 char), eventIdentifier varchar2(255 char), eventStage number(10,0), eventType number(10,0), hostIdentifier varchar2(255 char), initiatorName varchar2(255 char), initiatorOid varchar2(36 char), initiatorType number(10,0), message varchar2(1024 char), nodeIdentifier varchar2(255 char), outcome number(10,0), parameter varchar2(255 char), remoteHostAddress varchar2(255 char), result varchar2(255 char), sessionIdentifier varchar2(255 char), targetName varchar2(255 char), targetOid varchar2(36 char), targetOwnerName varchar2(255 char), targetOwnerOid varchar2(36 char), targetType number(10,0), taskIdentifier varchar2(255 char), taskOID varchar2(255 char), timestampValue timestamp, primary key (id)) initrans 30;
create table m_audit_item (changedItemPath varchar2(900 char) not null, record_id number(19,0) not null, primary key (changedItemPath, record_id)) initrans 30;
create table m_audit_prop_value (id number(19,0) generated as identity, name varchar2(255 char), record_id number(19,0), value varchar2(1024 char), primary key (id)) initrans 30;
create table m_audit_ref_value (id number(19,0) generated as identity, name varchar2(255 char), oid varchar2(255 char), record_id number(19,0), targetName_norm varchar2(255 char), targetName_orig varchar2(255 char), type varchar2(255 char), primary key (id)) initrans 30;

create index iTimestampValue on m_audit_event (timestampValue) initrans 30;
create index iChangedItemPath on m_audit_item (changedItemPath) initrans 30;
create index iAuditPropValRecordId on m_audit_prop_value (record_id) initrans 30;
create index iAuditRefValRecordId on m_audit_ref_value (record_id) initrans 30;

alter table m_audit_delta add constraint fk_audit_delta foreign key (record_id) references m_audit_event;
alter table m_audit_item add constraint fk_audit_item foreign key (record_id) references m_audit_event;
alter table m_audit_prop_value add constraint fk_audit_prop_value foreign key (record_id) references m_audit_event;
alter table m_audit_ref_value add constraint fk_audit_ref_value foreign key (record_id) references m_audit_event;
