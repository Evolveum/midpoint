CREATE INDEX iCertCampaignNameOrig
  ON m_acc_cert_campaign (name_orig) INITRANS 30;

CREATE INDEX iCertDefinitionNameOrig
  ON m_acc_cert_definition (name_orig) INITRANS 30;

CREATE INDEX iCaseNameOrig
  ON m_case (name_orig) INITRANS 30;

CREATE INDEX iConnectorNameOrig
  ON m_connector (name_orig) INITRANS 30;

CREATE INDEX iConnectorNameNorm
  ON m_connector (name_norm) INITRANS 30;

CREATE INDEX iConnectorHostNameOrig
  ON m_connector_host (name_orig) INITRANS 30;

CREATE INDEX iFormNameOrig
  ON m_form (name_orig) INITRANS 30;

CREATE INDEX iFunctionLibraryNameOrig
  ON m_function_library (name_orig) INITRANS 30;

CREATE INDEX iGenericObjectNameOrig
  ON m_generic_object (name_orig) INITRANS 30;

CREATE INDEX iLookupTableNameOrig
  ON m_lookup_table (name_orig) INITRANS 30;

CREATE INDEX iNodeNameOrig
  ON m_node (name_orig) INITRANS 30;

CREATE INDEX iObjectTemplateNameOrig
  ON m_object_template (name_orig) INITRANS 30;

CREATE INDEX iOrgNameOrig
  ON m_org (name_orig) INITRANS 30;

CREATE INDEX iReportNameOrig
  ON m_report (name_orig) INITRANS 30;

CREATE INDEX iReportOutputNameOrig
  ON m_report_output (name_orig) INITRANS 30;

CREATE INDEX iReportOutputNameNorm
  ON m_report_output (name_norm) INITRANS 30;

CREATE INDEX iResourceNameOrig
  ON m_resource (name_orig) INITRANS 30;

CREATE INDEX iRoleNameOrig
  ON m_role (name_orig) INITRANS 30;

CREATE INDEX iSecurityPolicyNameOrig
  ON m_security_policy (name_orig) INITRANS 30;

CREATE INDEX iSequenceNameOrig
  ON m_sequence (name_orig) INITRANS 30;

CREATE INDEX iServiceNameOrig
  ON m_service (name_orig) INITRANS 30;

CREATE INDEX iServiceNameNorm
  ON m_service (name_norm) INITRANS 30;

CREATE INDEX iShadowNameOrig
  ON m_shadow (name_orig) INITRANS 30;

CREATE INDEX iShadowNameNorm
  ON m_shadow (name_norm) INITRANS 30;

CREATE INDEX iSystemConfigurationNameOrig
  ON m_system_configuration (name_orig) INITRANS 30;

CREATE INDEX iTaskNameOrig
  ON m_task (name_orig) INITRANS 30;

CREATE INDEX iUserNameOrig
  ON m_user (name_orig) INITRANS 30;

CREATE INDEX iValuePolicyNameOrig
  ON m_value_policy (name_orig) INITRANS 30;

