create table m_abstract_role (
  approvalprocess varchar(255),
  requestable     boolean,
  oid             varchar(36) not null,
  primary key (oid)
);

create table m_acc_cert_campaign (
    definitionref_relation varchar(157),
    definitionref_targetoid varchar(36),
    definitionref_type integer,
    name_norm varchar(255),
    name_orig varchar(255),
    oid varchar(36) not null,
    primary key (oid)
);

create table m_acc_cert_definition (
    name_norm varchar(255),
    name_orig varchar(255),
    oid varchar(36) not null,
    primary key (oid)
);

create table m_assignment (
  id                      integer     not null,
  owner_oid               varchar(36) not null,
  administrativestatus    integer,
  archivetimestamp        timestamp,
  disablereason           varchar(255),
  disabletimestamp        timestamp,
  effectivestatus         integer,
  enabletimestamp         timestamp,
  validfrom               timestamp,
  validto                 timestamp,
  validitychangetimestamp timestamp,
  validitystatus          integer,
  assignmentowner         integer,
  createchannel           varchar(255),
  createtimestamp         timestamp,
  creatorref_relation     varchar(157),
  creatorref_targetoid    varchar(36),
  creatorref_type         integer,
  modifierref_relation    varchar(157),
  modifierref_targetoid   varchar(36),
  modifierref_type        integer,
  modifychannel           varchar(255),
  modifytimestamp         timestamp,
  ordervalue              integer,
  targetref_relation      varchar(157),
  targetref_targetoid     varchar(36),
  targetref_type          integer,
  tenantref_relation      varchar(157),
  tenantref_targetoid     varchar(36),
  tenantref_type          integer,
  extid                   integer,
  extoid                  varchar(36),
  primary key (id, owner_oid)
);

create table m_assignment_ext_boolean (
  ename                        varchar(157) not null,
  anycontainer_owner_id        integer      not null,
  anycontainer_owner_owner_oid varchar(36)  not null,
  booleanvalue                 boolean      not null,
  extensiontype                integer,
  dynamicdef                   boolean,
  etype                        varchar(157),
  valuetype                    integer,
  primary key (ename, anycontainer_owner_id, anycontainer_owner_owner_oid, booleanvalue)
);

create table m_assignment_ext_date (
  ename                        varchar(157) not null,
  anycontainer_owner_id        integer      not null,
  anycontainer_owner_owner_oid varchar(36)  not null,
  datevalue                    timestamp    not null,
  extensiontype                integer,
  dynamicdef                   boolean,
  etype                        varchar(157),
  valuetype                    integer,
  primary key (ename, anycontainer_owner_id, anycontainer_owner_owner_oid, datevalue)
);

create table m_assignment_ext_long (
  ename                        varchar(157) not null,
  anycontainer_owner_id        integer      not null,
  anycontainer_owner_owner_oid varchar(36)  not null,
  longvalue                    bigint       not null,
  extensiontype                integer,
  dynamicdef                   boolean,
  etype                        varchar(157),
  valuetype                    integer,
  primary key (ename, anycontainer_owner_id, anycontainer_owner_owner_oid, longvalue)
);

create table m_assignment_ext_poly (
  ename                        varchar(157) not null,
  anycontainer_owner_id        integer      not null,
  anycontainer_owner_owner_oid varchar(36)  not null,
  orig                         varchar(255) not null,
  extensiontype                integer,
  dynamicdef                   boolean,
  norm                         varchar(255),
  etype                        varchar(157),
  valuetype                    integer,
  primary key (ename, anycontainer_owner_id, anycontainer_owner_owner_oid, orig)
);

create table m_assignment_ext_reference (
  ename                        varchar(157) not null,
  anycontainer_owner_id        integer      not null,
  anycontainer_owner_owner_oid varchar(36)  not null,
  targetoid                    varchar(36)  not null,
  extensiontype                integer,
  dynamicdef                   boolean,
  relation                     varchar(157),
  targettype                   integer,
  etype                        varchar(157),
  valuetype                    integer,
  primary key (ename, anycontainer_owner_id, anycontainer_owner_owner_oid, targetoid)
);

create table m_assignment_ext_string (
  ename                        varchar(157) not null,
  anycontainer_owner_id        integer      not null,
  anycontainer_owner_owner_oid varchar(36)  not null,
  stringvalue                  varchar(255) not null,
  extensiontype                integer,
  dynamicdef                   boolean,
  etype                        varchar(157),
  valuetype                    integer,
  primary key (ename, anycontainer_owner_id, anycontainer_owner_owner_oid, stringvalue)
);

create table m_assignment_extension (
  owner_id        integer     not null,
  owner_owner_oid varchar(36) not null,
  booleanscount   smallint,
  datescount      smallint,
  longscount      smallint,
  polyscount      smallint,
  referencescount smallint,
  stringscount    smallint,
  primary key (owner_id, owner_owner_oid)
);

create table m_assignment_reference (
  owner_id        integer      not null,
  owner_owner_oid varchar(36)  not null,
  reference_type  integer      not null,
  relation        varchar(157) not null,
  targetoid       varchar(36)  not null,
  containertype   integer,
  primary key (owner_id, owner_owner_oid, reference_type, relation, targetoid)
);

create table m_audit_delta (
  checksum   varchar(32) not null,
  record_id  bigint      not null,
  delta      clob,
  deltaoid   varchar(36),
  deltatype  integer,
  fullresult clob,
  status     integer,
  primary key (checksum, record_id)
);

create table m_audit_event (
  id                bigint not null,
  channel           varchar(255),
  eventidentifier   varchar(255),
  eventstage        integer,
  eventtype         integer,
  hostidentifier    varchar(255),
  initiatorname     varchar(255),
  initiatoroid      varchar(36),
  message           varchar(1024),
  outcome           integer,
  parameter         varchar(255),
  result            varchar(255),
  sessionidentifier varchar(255),
  targetname        varchar(255),
  targetoid         varchar(36),
  targetownername   varchar(255),
  targetowneroid    varchar(36),
  targettype        integer,
  taskidentifier    varchar(255),
  taskoid           varchar(255),
  timestampvalue    timestamp,
  primary key (id)
);

create table m_connector (
  connectorbundle            varchar(255),
  connectorhostref_relation  varchar(157),
  connectorhostref_targetoid varchar(36),
  connectorhostref_type      integer,
  connectortype              varchar(255),
  connectorversion           varchar(255),
  framework                  varchar(255),
  name_norm                  varchar(255),
  name_orig                  varchar(255),
  oid                        varchar(36) not null,
  primary key (oid)
);

create table m_connector_host (
  hostname  varchar(255),
  name_norm varchar(255),
  name_orig varchar(255),
  port      varchar(255),
  oid       varchar(36) not null,
  primary key (oid)
);

create table m_connector_target_system (
  connector_oid    varchar(36) not null,
  targetsystemtype varchar(255)
);

create table m_exclusion (
  id                  integer     not null,
  owner_oid           varchar(36) not null,
  policy              integer,
  targetref_relation  varchar(157),
  targetref_targetoid varchar(36),
  targetref_type      integer,
  primary key (id, owner_oid)
);

create table m_focus (
  administrativestatus    integer,
  archivetimestamp        timestamp,
  disablereason           varchar(255),
  disabletimestamp        timestamp,
  effectivestatus         integer,
  enabletimestamp         timestamp,
  validfrom               timestamp,
  validto                 timestamp,
  validitychangetimestamp timestamp,
  validitystatus          integer,
  oid                     varchar(36) not null,
  primary key (oid)
);

create table m_generic_object (
  name_norm  varchar(255),
  name_orig  varchar(255),
  objecttype varchar(255),
  oid        varchar(36) not null,
  primary key (oid)
);

create table m_lookup_table (
  name_norm varchar(255),
  name_orig varchar(255),
  oid       varchar(36) not null,
  primary key (oid)
);

create table m_lookup_table_row (
  id                  integer     not null,
  owner_oid           varchar(36) not null,
  row_key             varchar(255),
  label_norm          varchar(255),
  label_orig          varchar(255),
  lastchangetimestamp timestamp,
  row_value           varchar(255),
  primary key (id, owner_oid)
);

create table m_node (
  name_norm      varchar(255),
  name_orig      varchar(255),
  nodeidentifier varchar(255),
  oid            varchar(36) not null,
  primary key (oid)
);

create table m_object (
  oid                   varchar(36) not null,
  booleanscount         smallint,
  createchannel         varchar(255),
  createtimestamp       timestamp,
  creatorref_relation   varchar(157),
  creatorref_targetoid  varchar(36),
  creatorref_type       integer,
  datescount            smallint,
  fullobject            blob,
  longscount            smallint,
  modifierref_relation  varchar(157),
  modifierref_targetoid varchar(36),
  modifierref_type      integer,
  modifychannel         varchar(255),
  modifytimestamp       timestamp,
  name_norm             varchar(255),
  name_orig             varchar(255),
  objecttypeclass       integer,
  polyscount            smallint,
  referencescount       smallint,
  stringscount          smallint,
  tenantref_relation    varchar(157),
  tenantref_targetoid   varchar(36),
  tenantref_type        integer,
  version               integer     not null,
  primary key (oid)
);

create table m_object_ext_boolean (
  ename        varchar(157) not null,
  owner_oid    varchar(36)  not null,
  ownertype    integer      not null,
  booleanvalue boolean      not null,
  dynamicdef   boolean,
  etype        varchar(157),
  valuetype    integer,
  primary key (ename, owner_oid, ownertype, booleanvalue)
);

create table m_object_ext_date (
  ename      varchar(157) not null,
  owner_oid  varchar(36)  not null,
  ownertype  integer      not null,
  datevalue  timestamp    not null,
  dynamicdef boolean,
  etype      varchar(157),
  valuetype  integer,
  primary key (ename, owner_oid, ownertype, datevalue)
);

create table m_object_ext_long (
  ename      varchar(157) not null,
  owner_oid  varchar(36)  not null,
  ownertype  integer      not null,
  longvalue  bigint       not null,
  dynamicdef boolean,
  etype      varchar(157),
  valuetype  integer,
  primary key (ename, owner_oid, ownertype, longvalue)
);

create table m_object_ext_poly (
  ename      varchar(157) not null,
  owner_oid  varchar(36)  not null,
  ownertype  integer      not null,
  orig       varchar(255) not null,
  dynamicdef boolean,
  norm       varchar(255),
  etype      varchar(157),
  valuetype  integer,
  primary key (ename, owner_oid, ownertype, orig)
);

create table m_object_ext_reference (
  ename      varchar(157) not null,
  owner_oid  varchar(36)  not null,
  ownertype  integer      not null,
  targetoid  varchar(36)  not null,
  dynamicdef boolean,
  relation   varchar(157),
  targettype integer,
  etype      varchar(157),
  valuetype  integer,
  primary key (ename, owner_oid, ownertype, targetoid)
);

create table m_object_ext_string (
  ename       varchar(157) not null,
  owner_oid   varchar(36)  not null,
  ownertype   integer      not null,
  stringvalue varchar(255) not null,
  dynamicdef  boolean,
  etype       varchar(157),
  valuetype   integer,
  primary key (ename, owner_oid, ownertype, stringvalue)
);

create table m_object_template (
  name_norm varchar(255),
  name_orig varchar(255),
  type      integer,
  oid       varchar(36) not null,
  primary key (oid)
);

create table m_org (
  costcenter       varchar(255),
  displayname_norm varchar(255),
  displayname_orig varchar(255),
  displayorder     integer,
  identifier       varchar(255),
  locality_norm    varchar(255),
  locality_orig    varchar(255),
  name_norm        varchar(255),
  name_orig        varchar(255),
  tenant           boolean,
  oid              varchar(36) not null,
  primary key (oid)
);

create table m_org_closure (
  ancestor_oid   varchar(36) not null,
  descendant_oid varchar(36) not null,
  val            integer,
  primary key (ancestor_oid, descendant_oid)
);

create table m_org_org_type (
  org_oid varchar(36) not null,
  orgtype varchar(255)
);

create table m_reference (
  owner_oid      varchar(36)  not null,
  reference_type integer      not null,
  relation       varchar(157) not null,
  targetoid      varchar(36)  not null,
  containertype  integer,
  primary key (owner_oid, reference_type, relation, targetoid)
);

create table m_report (
  export              integer,
  name_norm           varchar(255),
  name_orig           varchar(255),
  orientation         integer,
  parent              boolean,
  usehibernatesession boolean,
  oid                 varchar(36) not null,
  primary key (oid)
);

create table m_report_output (
  name_norm           varchar(255),
  name_orig           varchar(255),
  reportref_relation  varchar(157),
  reportref_targetoid varchar(36),
  reportref_type      integer,
  oid                 varchar(36) not null,
  primary key (oid)
);

create table m_resource (
  administrativestate        integer,
  connectorref_relation      varchar(157),
  connectorref_targetoid     varchar(36),
  connectorref_type          integer,
  name_norm                  varchar(255),
  name_orig                  varchar(255),
  o16_lastavailabilitystatus integer,
  oid                        varchar(36) not null,
  primary key (oid)
);

create table m_role (
  name_norm varchar(255),
  name_orig varchar(255),
  roletype  varchar(255),
  oid       varchar(36) not null,
  primary key (oid)
);

create table m_security_policy (
  name_norm varchar(255),
  name_orig varchar(255),
  oid       varchar(36) not null,
  primary key (oid)
);

create table m_shadow (
  attemptnumber                integer,
  dead                         boolean,
  exist                        boolean,
  failedoperationtype          integer,
  fullsynchronizationtimestamp timestamp,
  intent                       varchar(255),
  kind                         integer,
  name_norm                    varchar(255),
  name_orig                    varchar(255),
  objectclass                  varchar(157),
  resourceref_relation         varchar(157),
  resourceref_targetoid        varchar(36),
  resourceref_type             integer,
  status                       integer,
  synchronizationsituation     integer,
  synchronizationtimestamp     timestamp,
  oid                          varchar(36) not null,
  primary key (oid)
);

create table m_system_configuration (
  name_norm varchar(255),
  name_orig varchar(255),
  oid       varchar(36) not null,
  primary key (oid)
);

create table m_task (
  binding                integer,
  canrunonnode           varchar(255),
  category               varchar(255),
  completiontimestamp    timestamp,
  executionstatus        integer,
  handleruri             varchar(255),
  lastrunfinishtimestamp timestamp,
  lastrunstarttimestamp  timestamp,
  name_norm              varchar(255),
  name_orig              varchar(255),
  node                   varchar(255),
  objectref_relation     varchar(157),
  objectref_targetoid    varchar(36),
  objectref_type         integer,
  ownerref_relation      varchar(157),
  ownerref_targetoid     varchar(36),
  ownerref_type          integer,
  parent                 varchar(255),
  recurrence             integer,
  status                 integer,
  taskidentifier         varchar(255),
  threadstopaction       integer,
  waitingreason          integer,
  oid                    varchar(36) not null,
  primary key (oid)
);

create table m_task_dependent (
  task_oid  varchar(36) not null,
  dependent varchar(255)
);

create table m_trigger (
  id             integer     not null,
  owner_oid      varchar(36) not null,
  handleruri     varchar(255),
  timestampvalue timestamp,
  primary key (id, owner_oid)
);

create table m_user (
  additionalname_norm  varchar(255),
  additionalname_orig  varchar(255),
  costcenter           varchar(255),
  emailaddress         varchar(255),
  employeenumber       varchar(255),
  familyname_norm      varchar(255),
  familyname_orig      varchar(255),
  fullname_norm        varchar(255),
  fullname_orig        varchar(255),
  givenname_norm       varchar(255),
  givenname_orig       varchar(255),
  hasphoto             boolean     not null,
  honorificprefix_norm varchar(255),
  honorificprefix_orig varchar(255),
  honorificsuffix_norm varchar(255),
  honorificsuffix_orig varchar(255),
  locale               varchar(255),
  locality_norm        varchar(255),
  locality_orig        varchar(255),
  name_norm            varchar(255),
  name_orig            varchar(255),
  nickname_norm        varchar(255),
  nickname_orig        varchar(255),
  preferredlanguage    varchar(255),
  status               integer,
  telephonenumber      varchar(255),
  timezone             varchar(255),
  title_norm           varchar(255),
  title_orig           varchar(255),
  oid                  varchar(36) not null,
  primary key (oid)
);

create table m_user_employee_type (
  user_oid     varchar(36) not null,
  employeetype varchar(255)
);

create table m_user_organization (
  user_oid varchar(36) not null,
  norm     varchar(255),
  orig     varchar(255)
);

create table m_user_organizational_unit (
  user_oid varchar(36) not null,
  norm     varchar(255),
  orig     varchar(255)
);

create table m_user_photo (
  owner_oid varchar(36) not null,
  photo     blob,
  primary key (owner_oid)
);

create table m_value_policy (
  name_norm varchar(255),
  name_orig varchar(255),
  oid       varchar(36) not null,
  primary key (oid)
);

create index irequestable on m_abstract_role (requestable);

alter table m_acc_cert_campaign
    add constraint uc_acc_cert_campaign_name  unique (name_norm);

alter table m_acc_cert_definition
    add constraint uc_acc_cert_definition_name  unique (name_norm);

create index iassignmentadministrative on m_assignment (administrativestatus);

create index iassignmenteffective on m_assignment (effectivestatus);

create index iaextensionboolean on m_assignment_ext_boolean (extensiontype, ename, booleanvalue);

create index iaextensiondate on m_assignment_ext_date (extensiontype, ename, datevalue);

create index iaextensionlong on m_assignment_ext_long (extensiontype, ename, longvalue);

create index iaextensionpolystring on m_assignment_ext_poly (extensiontype, ename, orig);

create index iaextensionreference on m_assignment_ext_reference (extensiontype, ename, targetoid);

create index iaextensionstring on m_assignment_ext_string (extensiontype, ename, stringvalue);

create index iassignmentreferencetargetoid on m_assignment_reference (targetoid);

alter table m_connector_host
add constraint uc_connector_host_name unique (name_norm);

create index ifocusadministrative on m_focus (administrativestatus);

create index ifocuseffective on m_focus (effectivestatus);

alter table m_generic_object
add constraint uc_generic_object_name unique (name_norm);

alter table m_lookup_table
add constraint uc_lookup_name unique (name_norm);

alter table m_lookup_table_row
add constraint uc_row_key unique (row_key);

alter table m_node
add constraint uc_node_name unique (name_norm);

create index iobjectnameorig on m_object (name_orig);

create index iobjectnamenorm on m_object (name_norm);

create index iobjecttypeclass on m_object (objecttypeclass);

create index iobjectcreatetimestamp on m_object (createtimestamp);

create index iextensionboolean on m_object_ext_boolean (ownertype, ename, booleanvalue);

create index iextensionbooleandef on m_object_ext_boolean (owner_oid, ownertype);

create index iextensiondate on m_object_ext_date (ownertype, ename, datevalue);

create index iextensiondatedef on m_object_ext_date (owner_oid, ownertype);

create index iextensionlong on m_object_ext_long (ownertype, ename, longvalue);

create index iextensionlongdef on m_object_ext_long (owner_oid, ownertype);

create index iextensionpolystring on m_object_ext_poly (ownertype, ename, orig);

create index iextensionpolystringdef on m_object_ext_poly (owner_oid, ownertype);

create index iextensionreference on m_object_ext_reference (ownertype, ename, targetoid);

create index iextensionreferencedef on m_object_ext_reference (owner_oid, ownertype);

create index iextensionstring on m_object_ext_string (ownertype, ename, stringvalue);

create index iextensionstringdef on m_object_ext_string (owner_oid, ownertype);

alter table m_object_template
add constraint uc_object_template_name unique (name_norm);

alter table m_org
add constraint uc_org_name unique (name_norm);

create index idisplayorder on m_org (displayorder);

create index iancestor on m_org_closure (ancestor_oid);

create index idescendant on m_org_closure (descendant_oid);

create index idescendantancestor on m_org_closure (descendant_oid, ancestor_oid);

create index ireferencetargetoid on m_reference (targetoid);

alter table m_report
add constraint uc_report_name unique (name_norm);

create index ireportparent on m_report (parent);

alter table m_resource
add constraint uc_resource_name unique (name_norm);

alter table m_role
add constraint uc_role_name unique (name_norm);

alter table m_security_policy
add constraint uc_security_policy_name unique (name_norm);

create index ishadowresourceref on m_shadow (resourceref_targetoid);

create index ishadowdead on m_shadow (dead);

alter table m_system_configuration
add constraint uc_system_configuration_name unique (name_norm);

create index iparent on m_task (parent);

create index itriggertimestamp on m_trigger (timestampvalue);

alter table m_user
add constraint uc_user_name unique (name_norm);

create index iemployeenumber on m_user (employeenumber);

create index ifullname on m_user (fullname_orig);

create index ifamilyname on m_user (familyname_orig);

create index igivenname on m_user (givenname_orig);

create index ilocality on m_user (locality_orig);

alter table m_value_policy
add constraint uc_value_policy_name unique (name_norm);

alter table m_abstract_role
add constraint fk_abstract_role
foreign key (oid)
references m_focus;

alter table m_acc_cert_campaign
    add constraint fk_acc_cert_campaign
    foreign key (oid)
    references m_object;

alter table m_acc_cert_definition
    add constraint fk_acc_cert_definition
    foreign key (oid)
    references m_object;

alter table m_assignment
add constraint fk_assignment_owner
foreign key (owner_oid)
references m_object;

alter table m_assignment_ext_boolean
add constraint fk_assignment_ext_boolean
foreign key (anycontainer_owner_id, anycontainer_owner_owner_oid)
references m_assignment_extension;

alter table m_assignment_ext_date
add constraint fk_assignment_ext_date
foreign key (anycontainer_owner_id, anycontainer_owner_owner_oid)
references m_assignment_extension;

alter table m_assignment_ext_long
add constraint fk_assignment_ext_long
foreign key (anycontainer_owner_id, anycontainer_owner_owner_oid)
references m_assignment_extension;

alter table m_assignment_ext_poly
add constraint fk_assignment_ext_poly
foreign key (anycontainer_owner_id, anycontainer_owner_owner_oid)
references m_assignment_extension;

alter table m_assignment_ext_reference
add constraint fk_assignment_ext_reference
foreign key (anycontainer_owner_id, anycontainer_owner_owner_oid)
references m_assignment_extension;

alter table m_assignment_ext_string
add constraint fk_assignment_ext_string
foreign key (anycontainer_owner_id, anycontainer_owner_owner_oid)
references m_assignment_extension;

alter table m_assignment_reference
add constraint fk_assignment_reference
foreign key (owner_id, owner_owner_oid)
references m_assignment;

alter table m_audit_delta
add constraint fk_audit_delta
foreign key (record_id)
references m_audit_event;

alter table m_connector
add constraint fk_connector
foreign key (oid)
references m_object;

alter table m_connector_host
add constraint fk_connector_host
foreign key (oid)
references m_object;

alter table m_connector_target_system
add constraint fk_connector_target_system
foreign key (connector_oid)
references m_connector;

alter table m_exclusion
add constraint fk_exclusion_owner
foreign key (owner_oid)
references m_object;

alter table m_focus
add constraint fk_focus
foreign key (oid)
references m_object;

alter table m_generic_object
add constraint fk_generic_object
foreign key (oid)
references m_object;

alter table m_lookup_table
add constraint fk_lookup_table
foreign key (oid)
references m_object;

alter table m_lookup_table_row
add constraint fk_lookup_table_owner
foreign key (owner_oid)
references m_lookup_table;

alter table m_node
add constraint fk_node
foreign key (oid)
references m_object;

alter table m_object_ext_boolean
add constraint fk_object_ext_boolean
foreign key (owner_oid)
references m_object;

alter table m_object_ext_date
add constraint fk_object_ext_date
foreign key (owner_oid)
references m_object;

alter table m_object_ext_long
add constraint fk_object_ext_long
foreign key (owner_oid)
references m_object;

alter table m_object_ext_poly
add constraint fk_object_ext_poly
foreign key (owner_oid)
references m_object;

alter table m_object_ext_reference
add constraint fk_object_ext_reference
foreign key (owner_oid)
references m_object;

alter table m_object_ext_string
add constraint fk_object_ext_string
foreign key (owner_oid)
references m_object;

alter table m_object_template
add constraint fk_object_template
foreign key (oid)
references m_object;

alter table m_org
add constraint fk_org
foreign key (oid)
references m_abstract_role;

alter table m_org_closure
add constraint fk_ancestor
foreign key (ancestor_oid)
references m_object;

alter table m_org_closure
add constraint fk_descendant
foreign key (descendant_oid)
references m_object;

alter table m_org_org_type
add constraint fk_org_org_type
foreign key (org_oid)
references m_org;

alter table m_reference
add constraint fk_reference_owner
foreign key (owner_oid)
references m_object;

alter table m_report
add constraint fk_report
foreign key (oid)
references m_object;

alter table m_report_output
add constraint fk_report_output
foreign key (oid)
references m_object;

alter table m_resource
add constraint fk_resource
foreign key (oid)
references m_object;

alter table m_role
add constraint fk_role
foreign key (oid)
references m_abstract_role;

alter table m_security_policy
add constraint fk_security_policy
foreign key (oid)
references m_object;

alter table m_shadow
add constraint fk_shadow
foreign key (oid)
references m_object;

alter table m_system_configuration
add constraint fk_system_configuration
foreign key (oid)
references m_object;

alter table m_task
add constraint fk_task
foreign key (oid)
references m_object;

alter table m_task_dependent
add constraint fk_task_dependent
foreign key (task_oid)
references m_task;

alter table m_trigger
add constraint fk_trigger_owner
foreign key (owner_oid)
references m_object;

alter table m_user
add constraint fk_user
foreign key (oid)
references m_focus;

alter table m_user_employee_type
add constraint fk_user_employee_type
foreign key (user_oid)
references m_user;

alter table m_user_organization
add constraint fk_user_organization
foreign key (user_oid)
references m_user;

alter table m_user_organizational_unit
add constraint fk_user_org_unit
foreign key (user_oid)
references m_user;

alter table m_user_photo
add constraint fk_user_photo
foreign key (owner_oid)
references m_user;

alter table m_value_policy
add constraint fk_value_policy
foreign key (oid)
references m_object;

create sequence hibernate_sequence start with 1 increment by 1;
