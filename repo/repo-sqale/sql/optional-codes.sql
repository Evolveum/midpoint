-- @formatter:off
-- Describes c_object.objectClassType
CREATE TABLE c_objtype (
    id INT PRIMARY KEY,
    name VARCHAR(64),
    table_name VARCHAR(64)
);

-- Based on RObjectType
INSERT INTO c_objtype VALUES (0, 'CONNECTOR', 'm_connector');
INSERT INTO c_objtype VALUES (1, 'CONNECTOR_HOST', 'm_connector_host');
INSERT INTO c_objtype VALUES (2, 'GENERIC_OBJECT', 'm_generic_object');
INSERT INTO c_objtype VALUES (3, 'OBJECT', 'm_object');
INSERT INTO c_objtype VALUES (4, 'VALUE_POLICY', 'm_value_policy');
INSERT INTO c_objtype VALUES (5, 'RESOURCE', 'm_resource');
INSERT INTO c_objtype VALUES (6, 'SHADOW', 'm_shadow');
INSERT INTO c_objtype VALUES (7, 'ROLE', 'm_role');
INSERT INTO c_objtype VALUES (8, 'SYSTEM_CONFIGURATION', 'm_system_configuration');
INSERT INTO c_objtype VALUES (9, 'TASK', 'm_task');
INSERT INTO c_objtype VALUES (10, 'USER', 'm_user');
INSERT INTO c_objtype VALUES (11, 'REPORT', 'm_report');
INSERT INTO c_objtype VALUES (12, 'REPORT_DATA', 'm_report_output');
INSERT INTO c_objtype VALUES (13, 'OBJECT_TEMPLATE', 'm_object_template');
INSERT INTO c_objtype VALUES (14, 'NODE', 'm_node');
INSERT INTO c_objtype VALUES (15, 'ORG', 'm_org');
INSERT INTO c_objtype VALUES (16, 'ABSTRACT_ROLE', 'm_abstract_role');
INSERT INTO c_objtype VALUES (17, 'FOCUS', 'm_focus');
INSERT INTO c_objtype VALUES (18, 'ASSIGNMENT_HOLDER', NULL);
INSERT INTO c_objtype VALUES (19, 'SECURITY_POLICY', 'm_security_policy');
INSERT INTO c_objtype VALUES (20, 'LOOKUP_TABLE', 'm_lookup_table');
INSERT INTO c_objtype VALUES (21, 'ACCESS_CERTIFICATION_DEFINITION', 'm_acc_cert_definition');
INSERT INTO c_objtype VALUES (22, 'ACCESS_CERTIFICATION_CAMPAIGN', 'm_acc_cert_campaign');
INSERT INTO c_objtype VALUES (23, 'SEQUENCE', 'm_sequence');
INSERT INTO c_objtype VALUES (24, 'SERVICE', 'm_service');
INSERT INTO c_objtype VALUES (25, 'FORM', 'm_form');
INSERT INTO c_objtype VALUES (26, 'CASE', 'm_case');
INSERT INTO c_objtype VALUES (27, 'FUNCTION_LIBRARY', 'm_function_library');
INSERT INTO c_objtype VALUES (28, 'OBJECT_COLLECTION', 'm_object_collection');
INSERT INTO c_objtype VALUES (29, 'ARCHETYPE', 'm_archetype');
INSERT INTO c_objtype VALUES (30, 'DASHBOARD', 'm_dashboard');

-- Describes c_reference.referenceType
CREATE TABLE c_reftype (
    id INT PRIMARY KEY,
    name VARCHAR(64)
);

-- Based on RReferenceType
INSERT INTO c_reftype VALUES (0, 'OBJECT_PARENT_ORG');
INSERT INTO c_reftype VALUES (1, 'USER_ACCOUNT');
INSERT INTO c_reftype VALUES (2, 'RESOURCE_BUSINESS_CONFIGURATION_APPROVER');
INSERT INTO c_reftype VALUES (3, '(DEPRECATED) ROLE_APPROVER');
INSERT INTO c_reftype VALUES (4, '(DEPRECATED) SYSTEM_CONFIGURATION_ORG_ROOT');
INSERT INTO c_reftype VALUES (5, 'CREATE_APPROVER');
INSERT INTO c_reftype VALUES (6, 'MODIFY_APPROVER');
INSERT INTO c_reftype VALUES (7, 'INCLUDE');
INSERT INTO c_reftype VALUES (8, 'ROLE_MEMBER');
INSERT INTO c_reftype VALUES (9, 'DELEGATED');
INSERT INTO c_reftype VALUES (10, 'PERSONA');
INSERT INTO c_reftype VALUES (11, 'ARCHETYPE');
