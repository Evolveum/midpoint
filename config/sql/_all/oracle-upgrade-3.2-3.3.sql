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
