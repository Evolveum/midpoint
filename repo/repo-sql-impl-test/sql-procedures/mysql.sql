DELIMITER $$

DROP FUNCTION IF EXISTS `cleanupTestDatabase`$$

CREATE FUNCTION `cleanupTestDatabase`()
  RETURNS TINYINT(1)
DETERMINISTIC
  BEGIN
    DECLARE run TINYINT DEFAULT 0;

    DELETE FROM m_object_text_info;
    DELETE FROM m_operation_execution;
    DELETE FROM m_sequence;
    DELETE FROM m_acc_cert_wi_reference;
    DELETE FROM m_acc_cert_wi;
    DELETE FROM m_acc_cert_case;
    DELETE FROM m_acc_cert_campaign;
    DELETE FROM m_acc_cert_definition;
    DELETE FROM m_audit_prop_value;
    DELETE FROM m_audit_ref_value;
    DELETE FROM m_audit_delta;
    DELETE FROM m_audit_item;
    DELETE FROM m_audit_event;
    DELETE FROM m_object_ext_date;
    DELETE FROM m_object_ext_long;
    DELETE FROM m_object_ext_string;
    DELETE FROM m_object_ext_poly;
    DELETE FROM m_object_ext_reference;
    DELETE FROM m_object_ext_boolean;
    DELETE FROM m_reference;
    DELETE FROM m_assignment_ext_date;
    DELETE FROM m_assignment_ext_long;
    DELETE FROM m_assignment_ext_poly;
    DELETE FROM m_assignment_ext_reference;
    DELETE FROM m_assignment_ext_string;
    DELETE FROM m_assignment_ext_boolean;
    DELETE FROM m_assignment_extension;
    DELETE FROM m_assignment_reference;
    DELETE FROM m_assignment_policy_situation;
    DELETE FROM m_assignment;
    DELETE FROM m_connector_target_system;
    DELETE FROM m_connector;
    DELETE FROM m_connector_host;
    DELETE FROM m_lookup_table_row;
    DELETE FROM m_lookup_table;
    DELETE FROM m_node;
    DELETE FROM m_shadow;
    DELETE FROM m_task_dependent;
    DELETE FROM m_task;
    DELETE FROM m_object_template;
    DELETE FROM m_value_policy;
    DELETE FROM m_resource;
    DELETE FROM m_user_employee_type;
    DELETE FROM m_user_organization;
    DELETE FROM m_user_organizational_unit;
    DELETE FROM m_focus_photo;
    DELETE FROM m_focus_policy_situation;
    DELETE FROM m_user;
    DELETE FROM m_report;
    DELETE FROM m_report_output;
    DELETE FROM m_org_org_type;
    DELETE FROM m_org;
    DELETE FROM m_org_closure;
    DELETE FROM m_role;
    DELETE FROM m_service_type;
    DELETE FROM m_service;
    DELETE FROM m_abstract_role;
    DELETE FROM m_system_configuration;
    DELETE FROM m_generic_object;
    DELETE FROM m_trigger;
    DELETE FROM m_focus;
    DELETE FROM m_security_policy;
    DELETE FROM m_form;
    DELETE FROM m_case;
    DELETE FROM m_function_library;
    DELETE FROM m_ext_item;
    DELETE FROM m_object_subtype;
    DELETE FROM m_object;

    RETURN run;
  END$$

DELIMITER ;