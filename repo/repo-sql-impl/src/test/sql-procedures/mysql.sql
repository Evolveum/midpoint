create function cleanupTestDatabase() returns integer
begin
  truncate table m_account_shadow ;
  truncate table m_any ;
  truncate table m_any_clob ;
  truncate table m_any_date ;
  truncate table m_any_long ;
  truncate table m_any_string ;
  truncate table m_assignment ;
  truncate table m_audit_delta ;
  truncate table m_audit_event ;
  truncate table m_connector ;
  truncate table m_connector_host ;
  truncate table m_connector_target_system ;
  truncate table m_container ;
  truncate table m_exclusion ;
  truncate table m_generic_object ;
  truncate table m_node ;
  truncate table m_object ;
  truncate table m_object_org_ref ;
  truncate table m_operation_result ;
  truncate table m_org ;
  truncate table m_org_closure ;
  truncate table m_org_org_type ;
  truncate table m_org_sys_config ;
  truncate table m_password_policy ;
  truncate table m_reference ;
  truncate table m_resource ;
  truncate table m_resource_approver_ref ;
  truncate table m_resource_shadow ;
  truncate table m_role ;
  truncate table m_sync_situation_description ;
  truncate table m_system_configuration ;
  truncate table m_task ;
  truncate table m_user ;
  truncate table m_user_employee_type ;
  truncate table m_user_organizational_unit ;
  truncate table m_user_template ;
  update hibernate_sequence set next_val=1;
  
  return 0;
end