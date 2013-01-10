CREATE OR REPLACE
PROCEDURE cleanupTestDatabaseProc
IS
  BEGIN
    EXECUTE immediate 'delete from m_audit_delta';
    EXECUTE immediate 'delete from m_audit_event';
    EXECUTE immediate 'delete from m_any_clob';
    EXECUTE immediate 'delete from m_any_date';
    EXECUTE immediate 'delete from m_any_long';
    EXECUTE immediate 'delete from m_any_string';
    EXECUTE immediate 'delete from m_any';
    EXECUTE immediate 'delete from m_reference';
    EXECUTE immediate 'delete from m_assignment';
    EXECUTE immediate 'delete from m_exclusion';
    EXECUTE immediate 'delete from m_operation_result';
    EXECUTE immediate 'delete from m_connector_target_system';
    EXECUTE immediate 'delete from m_connector';
    EXECUTE immediate 'delete from m_connector_host';
    EXECUTE immediate 'delete from m_node';
    EXECUTE immediate 'delete from m_account_shadow';
    EXECUTE immediate 'delete from m_sync_situation_description';
    EXECUTE immediate 'delete from m_resource_shadow';
    EXECUTE immediate 'delete from m_role';
    EXECUTE immediate 'delete from m_task';
    EXECUTE immediate 'delete from m_user_template';
    EXECUTE immediate 'delete from m_password_policy';
    EXECUTE immediate 'delete from m_resource_approver_ref';
    EXECUTE immediate 'delete from m_resource';
    EXECUTE immediate 'delete from m_user_employee_type';
    EXECUTE immediate 'delete from m_user_organizational_unit';
    EXECUTE immediate 'delete from m_user';
    EXECUTE immediate 'delete from m_org_org_type';
    EXECUTE immediate 'delete from m_org';
    EXECUTE immediate 'delete from m_org_sys_config';
    EXECUTE immediate 'delete from m_system_configuration';
    EXECUTE immediate 'delete from m_generic_object';
    EXECUTE immediate 'delete from m_org_closure';
    EXECUTE immediate 'delete from m_object_org_ref';
    EXECUTE immediate 'delete from m_object';
    EXECUTE immediate 'delete from m_container';
    EXECUTE immediate 'drop sequence "BAMBOO"."HIBERNATE_SEQUENCE"';
    EXECUTE immediate 'create sequence "BAMBOO"."HIBERNATE_SEQUENCE" MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE';
  END cleanupTestDatabaseProc;