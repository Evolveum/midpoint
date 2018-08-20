CREATE INDEX iCertCampaignNameOrig
  ON m_acc_cert_campaign (name_orig);

CREATE INDEX iCertDefinitionNameOrig
  ON m_acc_cert_definition (name_orig);

CREATE INDEX iCaseNameOrig
  ON m_case (name_orig);

CREATE INDEX iConnectorNameOrig
  ON m_connector (name_orig);

CREATE INDEX iConnectorNameNorm
  ON m_connector (name_norm);

CREATE INDEX iConnectorHostNameOrig
  ON m_connector_host (name_orig);

CREATE INDEX iFormNameOrig
  ON m_form (name_orig);

CREATE INDEX iFunctionLibraryNameOrig
  ON m_function_library (name_orig);

CREATE INDEX iGenericObjectNameOrig
  ON m_generic_object (name_orig);

CREATE INDEX iLookupTableNameOrig
  ON m_lookup_table (name_orig);

CREATE INDEX iNodeNameOrig
  ON m_node (name_orig);

CREATE INDEX iObjectTemplateNameOrig
  ON m_object_template (name_orig);

CREATE INDEX iOrgNameOrig
  ON m_org (name_orig);

CREATE INDEX iReportNameOrig
  ON m_report (name_orig);

CREATE INDEX iReportOutputNameOrig
  ON m_report_output (name_orig);

CREATE INDEX iReportOutputNameNorm
  ON m_report_output (name_norm);

CREATE INDEX iResourceNameOrig
  ON m_resource (name_orig);

CREATE INDEX iRoleNameOrig
  ON m_role (name_orig);

CREATE INDEX iSecurityPolicyNameOrig
  ON m_security_policy (name_orig);

CREATE INDEX iSequenceNameOrig
  ON m_sequence (name_orig);

CREATE INDEX iServiceNameOrig
  ON m_service (name_orig);

CREATE INDEX iServiceNameNorm
  ON m_service (name_norm);

CREATE INDEX iShadowNameOrig
  ON m_shadow (name_orig);

CREATE INDEX iShadowNameNorm
  ON m_shadow (name_norm);

CREATE INDEX iSystemConfigurationNameOrig
  ON m_system_configuration (name_orig);

CREATE INDEX iTaskNameOrig
  ON m_task (name_orig);

CREATE INDEX iUserNameOrig
  ON m_user (name_orig);

CREATE INDEX iValuePolicyNameOrig
  ON m_value_policy (name_orig);
