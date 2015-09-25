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
