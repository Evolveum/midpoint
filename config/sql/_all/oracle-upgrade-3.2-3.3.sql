create table m_sequence (
    name_norm varchar2(255 char),
    name_orig varchar2(255 char),
    oid varchar2(36 char) not null,
    primary key (oid)
);

alter table m_sequence
    add constraint uc_sequence_name  unique (name_norm);

alter table m_sequence
    add constraint fk_sequence
    foreign key (oid)
    references m_object;



alter table m_user_photo
    drop constraint fk_user_photo;

rename m_user_photo to m_focus_photo;

alter table m_focus_photo
    add constraint fk_focus_photo
    foreign key (owner_oid)
    references m_focus;

alter table m_focus add hasPhoto number(1,0) default 0 not null;
update m_focus set hasPhoto = 0;
update m_focus set hasPhoto = (select hasPhoto from m_user where m_user.oid = m_focus.oid)
    where m_focus.oid in (select oid from m_user);
alter table m_focus modify hasPhoto default null;

alter table m_user drop column hasPhoto;
