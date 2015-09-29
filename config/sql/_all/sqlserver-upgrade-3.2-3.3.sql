create table m_sequence (
    name_norm nvarchar(255) collate database_default,
    name_orig nvarchar(255) collate database_default,
    oid nvarchar(36) collate database_default not null,
    primary key (oid)
);

alter table m_sequence
    add constraint uc_sequence_name  unique (name_norm);

alter table m_sequence
    add constraint fk_sequence
    foreign key (oid)
    references m_object;

exec sp_rename m_user_photo, m_focus_photo;

alter table m_focus add hasPhoto bit not null constraint default_constraint default 0;

update m_focus set hasPhoto = 0;
    update m_focus set hasPhoto = (select hasPhoto from m_user where m_user.oid = m_focus.oid)
    where m_focus.oid in (select oid from m_user);

alter table m_focus drop constraint default_constraint;

alter table m_user drop column hasPhoto;

alter table m_focus_photo
    drop constraint fk_user_photo;

alter table m_focus_photo
    add constraint fk_focus_photo
    foreign key (owner_oid)
    references m_focus;

