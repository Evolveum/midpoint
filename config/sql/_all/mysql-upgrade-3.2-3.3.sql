create table m_sequence (
    name_norm varchar(255),
    name_orig varchar(255),
    oid varchar(36) not null,
    primary key (oid)
) DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE=InnoDB;

alter table m_sequence
    add constraint uc_sequence_name  unique (name_norm);

alter table m_sequence
    add constraint fk_sequence
    foreign key (oid)
    references m_object (oid);

rename table m_user_photo to m_focus_photo;

alter table m_focus add hasPhoto bit not null default 0;

update m_focus set hasPhoto = 0;
update m_focus set hasPhoto = (select hasPhoto from m_user where m_user.oid = m_focus.oid)
where m_focus.oid in (select oid from m_user);

alter table m_focus alter column hasPhoto drop default;

alter table m_user drop column hasPhoto;

alter table m_focus_photo
  drop foreign key fk_user_photo;

alter table m_focus_photo
  add constraint fk_focus_photo
  foreign key (owner_oid)
  references m_focus (oid);

