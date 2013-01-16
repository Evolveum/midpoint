create or replace function cleanupTestDatabase() returns integer as $$
begin
  truncate table m_account_shadow restart identity cascade;
  truncate table m_any restart identity cascade;
  truncate table m_any_clob restart identity cascade;
  truncate table m_any_date restart identity cascade;
  truncate table m_any_long restart identity cascade;
  truncate table m_any_string restart identity cascade;
  truncate table m_any_reference restart identity cascade;
  truncate table m_assignment restart identity cascade;
  truncate table m_audit_delta restart identity cascade;
  truncate table m_audit_event restart identity cascade;
  truncate table m_connector restart identity cascade;
  truncate table m_connector_host restart identity cascade;
  truncate table m_connector_target_system restart identity cascade;
  truncate table m_container restart identity cascade;
  truncate table m_exclusion restart identity cascade;
  truncate table m_generic_object restart identity cascade;
  truncate table m_node restart identity cascade;
  truncate table m_object restart identity cascade;
  truncate table m_object_org_ref restart identity cascade;
  truncate table m_operation_result restart identity cascade;
  truncate table m_org restart identity cascade;
  truncate table m_org_closure restart identity cascade;
  truncate table m_org_org_type restart identity cascade;
  truncate table m_org_sys_config restart identity cascade;
  truncate table m_password_policy restart identity cascade;
  truncate table m_reference restart identity cascade;
  truncate table m_resource restart identity cascade;
  truncate table m_resource_approver_ref restart identity cascade;
  truncate table m_resource_shadow restart identity cascade;
  truncate table m_role restart identity cascade;
  truncate table m_sync_situation_description restart identity cascade;
  truncate table m_system_configuration restart identity cascade;
  truncate table m_task restart identity cascade;
  truncate table m_user restart identity cascade;
  truncate table m_user_employee_type restart identity cascade;
  truncate table m_user_organizational_unit restart identity cascade;
  truncate table m_user_template restart identity cascade;

  alter sequence hibernate_sequence restart with 1;

  return 0;
end;
$$ language plpgsql;