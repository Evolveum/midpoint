DELIMITER $$

DROP FUNCTION IF EXISTS `cleanupTestDatabase`$$

CREATE FUNCTION `cleanupTestDatabase`() RETURNS TINYINT(1)
    DETERMINISTIC
BEGIN
	DECLARE run TINYINT DEFAULT 0;

  delete from m_audit_delta;
  delete from m_audit_event;

  delete from m_any_clob;
  delete from m_any_date;
  delete from m_any_long;
  delete from m_any_string;
  delete from m_any_reference;
  delete from m_any;

  delete from m_reference;

  delete from m_assignment;
  delete from m_exclusion;
  delete from m_operation_result;

  delete from m_connector_target_system;
  delete from m_connector;
  delete from m_connector_host;
  delete from m_node;
  delete from m_account_shadow;
  delete from m_sync_situation_description;
  delete from m_resource_shadow;
  delete from m_role;
  delete from m_task;
  delete from m_user_template;
  delete from m_password_policy;
  delete from m_resource_approver_ref;
  delete from m_resource;
  delete from m_user_employee_type;
  delete from m_user_organizational_unit;
  delete from m_user;
  delete from m_org_org_type;
  delete from m_org;
  delete from m_org_sys_config;
  delete from m_system_configuration;
  delete from m_generic_object;

  delete from m_org_closure;

  delete from m_object_org_ref;
  delete from m_object;
  delete from m_container;

  update hibernate_sequence set next_val=1;

	RETURN run;
END$$

DELIMITER ;