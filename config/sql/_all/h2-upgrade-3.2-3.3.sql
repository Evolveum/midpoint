create table m_sequence (
    name_norm varchar(255),
    name_orig varchar(255),
    oid varchar(36) not null,
    primary key (oid)
);

alter table m_sequence
    add constraint uc_sequence_name unique (name_norm);

alter table m_sequence
    add constraint fk_sequence
    foreign key (oid)
    references m_object;

alter table m_user_photo
    drop constraint fk_user_photo;

alter table m_user_photo rename to m_focus_photo;

alter table m_focus_photo
    add constraint fk_focus_photo
    foreign key (owner_oid)
    references m_focus;

alter table m_focus add hasPhoto boolean not null default false;

update m_focus set hasPhoto = false;
update m_focus set hasPhoto = (select hasPhoto from m_user where m_user.oid = m_focus.oid)
    where m_focus.oid in (select oid from m_user);

alter table m_user drop column hasPhoto;

alter table m_assignment add orgRef_relation varchar(157);
alter table m_assignment add orgRef_targetOid varchar(36);
alter table m_assignment add orgRef_type integer;
alter table m_assignment add resourceRef_relation varchar(157);
alter table m_assignment add resourceRef_targetOid varchar(36);
alter table m_assignment add resourceRef_type integer;

create index iTargetRefTargetOid on m_assignment (targetRef_targetOid);
create index iTenantRefTargetOid on m_assignment (tenantRef_targetOid);
create index iOrgRefTargetOid on m_assignment (orgRef_targetOid);
create index iResourceRefTargetOid on m_assignment (resourceRef_targetOid);

create index iTimestampValue on m_audit_event (timestampValue);
