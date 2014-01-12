SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		lazyman
-- Create date: 15.01.2013
-- Description:	used for db cleanup during testing, used by bamboo build system
-- =============================================
CREATE PROCEDURE cleanupTestDatabaseProc
AS
  BEGIN
    SET NOCOUNT ON;

    DELETE FROM m_audit_delta;
    DELETE FROM m_audit_event;

    DELETE FROM m_any_clob;
    DELETE FROM m_any_date;
    DELETE FROM m_any_long;
    DELETE FROM m_any_string;
    DELETE FROM m_any_poly_string;
    DELETE FROM m_any_reference;
    DELETE FROM m_any;

    DELETE FROM m_reference;

    DELETE FROM m_assignment;
    DELETE FROM m_exclusion;
    DELETE FROM m_operation_result;

    DELETE FROM m_connector_target_system;
    DELETE FROM m_connector;
    DELETE FROM m_connector_host;
    DELETE FROM m_node;
    DELETE FROM m_sync_situation_description;
    DELETE FROM m_shadow;
    DELETE FROM m_task_dependent;
    DELETE FROM m_task;
    DELETE FROM m_object_template;
    DELETE FROM m_value_policy;
    DELETE FROM m_resource;
    DELETE FROM m_user_employee_type;
    DELETE FROM m_user_organization;
    DELETE FROM m_user_organizational_unit;
    DELETE FROM m_user;
	DELETE FROM m_report;
	DELETE FROM m_report_output;
    DELETE FROM m_org_org_type;
    DELETE FROM m_authorization_action;
    DELETE FROM m_authorization;
    DELETE FROM m_org;
    DELETE FROM m_role;
    DELETE FROM m_abstract_role;
    DELETE FROM m_system_configuration;
    DELETE FROM m_generic_object;
    DELETE FROM m_trigger;

    DELETE FROM m_org_incorrect;
    DELETE FROM m_org_closure;

    DELETE FROM m_metadata;

    DELETE FROM m_focus;
    DELETE FROM m_object;
    DELETE FROM m_container;

    UPDATE hibernate_sequence SET next_val=1;
  END
GO
