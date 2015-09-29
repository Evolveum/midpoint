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

alter table m_focus add hasPhoto bit not null after validityStatus;

alter table m_user drop hasPhoto;
