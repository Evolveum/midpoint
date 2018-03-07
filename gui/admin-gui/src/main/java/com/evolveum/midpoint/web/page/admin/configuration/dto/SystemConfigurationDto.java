/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;

/**
 * @author lazyman
 */
public class SystemConfigurationDto implements Serializable {

	public static final String F_ASSIGNMENTPOLICYENFORCEMENT_LEVEL = "aepLevel";
	public static final String F_AUDIT_CLEANUP_AGE = "auditCleanupAge";
	public static final String F_AUDIT_CLEANUP_RECORDS = "auditCleanupRecords";
	public static final String F_TASK_CLEANUP_AGE = "taskCleanupAge";
	public static final String F_TASK_CLEANUP_RECORDS = "taskCleanupRecords";
	public static final String F_CAMPAIGN_CLEANUP_AGE = "campaignCleanupAge";
	public static final String F_CAMPAIGN_CLEANUP_RECORDS = "campaignCleanupRecords";
	public static final String F_REPORT_CLEANUP_AGE = "reportCleanupAge";
	public static final String F_REPORT_CLEANUP_RECORDS = "reportCleanupRecords";
	public static final String F_RESULT_CLEANUP_AGE = "resultCleanupAge";
	public static final String F_RESULT_CLEANUP_RECORDS = "resultCleanupRecords";
	public static final String F_PASSWORD_POLICY = "passPolicyDto";
	public static final String F_SECURITY_POLICY = "securityPolicyDto";
	public static final String F_OBJECT_TEMPLATE = "objectTemplateDto";
	public static final String F_OBJECT_POLICY_LIST = "objectPolicyList";
	public static final String F_NOTIFICATION_CONFIGURATION = "notificationConfig";
	public static final String F_ENABLE_EXPERIMENTAL_CODE = "enableExperimentalCode";
	public static final String F_USER_DASHBOARD_LINK = "userDashboardLink";
	public static final String F_ADDITIONAL_MENU_LINK = "additionalMenuLink";
	public static final String F_DEPLOYMENT_INFORMATION = "deploymentInformation";
	private AEPlevel aepLevel;

	private class CleanupInfo implements Serializable {
		String ageValue;
		Integer records;

		void initFrom(CleanupPolicyType config) {
			if (config != null) {
				ageValue = config.getMaxAge() != null ? config.getMaxAge().toString() : null;
				records = config.getMaxRecords();
			} else {
				ageValue = null;
				records = null;
			}
		}

		CleanupPolicyType toConfig() {
			if (StringUtils.isEmpty(ageValue) && records == null) {
				return null;
			}
			CleanupPolicyType rv = new CleanupPolicyType();
			rv.setMaxAge(XmlTypeConverter.createDuration(MiscUtil.nullIfEmpty(ageValue)));
			rv.setMaxRecords(records);
			return rv;
		}
	}

	private final CleanupInfo auditCleanup = new CleanupInfo();
	private final CleanupInfo taskCleanup = new CleanupInfo();
	private final CleanupInfo campaignsCleanup = new CleanupInfo();
	private final CleanupInfo reportsCleanup = new CleanupInfo();
	private final CleanupInfo resultsCleanup = new CleanupInfo();

	private DeploymentInformationType deploymentInformation;

	private Boolean enableExperimentalCode;

	private ObjectViewDto<ValuePolicyType> passPolicyDto;
	private ObjectViewDto<SecurityPolicyType> securityPolicyDto;
	private List<ObjectPolicyConfigurationTypeDto> objectPolicyList;
	private NotificationConfigurationDto notificationConfig;
	private List<RichHyperlinkType> userDashboardLink;
	private List<RichHyperlinkType> additionalMenuLink;

	private LoggingDto loggingConfig;
	private ProfilingDto profilingDto;

	private SystemConfigurationType oldObject;

	private List<AppenderConfiguration> appenders = new ArrayList<>();

	public SystemConfigurationDto() {
		this(null);
	}

	public SystemConfigurationDto(PrismObject<SystemConfigurationType> config) {
		oldObject = config.clone().asObjectable();
		init(config.asObjectable());
	}

	private void init(SystemConfigurationType config) {
		if (config == null) {
			return;
		}

		if (config.getGlobalAccountSynchronizationSettings() != null) {
			AssignmentPolicyEnforcementType globalAEP = config.getGlobalAccountSynchronizationSettings()
					.getAssignmentPolicyEnforcement();
			aepLevel = AEPlevel.fromAEPLevelType(globalAEP);
		}

		deploymentInformation = config.getDeploymentInformation();

		auditCleanup.initFrom(config.getCleanupPolicy() != null ? config.getCleanupPolicy().getAuditRecords() : null);
		taskCleanup.initFrom(config.getCleanupPolicy() != null ? config.getCleanupPolicy().getClosedTasks() : null);
		campaignsCleanup.initFrom(config.getCleanupPolicy() != null ? config.getCleanupPolicy().getClosedCertificationCampaigns() : null);
		reportsCleanup.initFrom(config.getCleanupPolicy() != null ? config.getCleanupPolicy().getOutputReports() : null);
		resultsCleanup.initFrom(config.getCleanupPolicy() != null ? config.getCleanupPolicy().getObjectResults() : null);

		passPolicyDto = loadPasswordPolicy(config);
		securityPolicyDto = loadSecurityPolicy(config);

		objectPolicyList = new ArrayList<>();
		List<ObjectPolicyConfigurationType> objectPolicies = config.getDefaultObjectPolicyConfiguration();
		if (objectPolicies != null && !objectPolicies.isEmpty()) {
			for (ObjectPolicyConfigurationType policy : objectPolicies) {
				objectPolicyList.add(new ObjectPolicyConfigurationTypeDto(policy));
			}
		} else {
			objectPolicyList.add(new ObjectPolicyConfigurationTypeDto());
		}

		// NOTIFICATIONS
		if (config.getNotificationConfiguration() != null) {
			notificationConfig = new NotificationConfigurationDto(config.getNotificationConfiguration());
		} else {
			notificationConfig = new NotificationConfigurationDto();
		}

		// LOGGING
		LoggingConfigurationType logging = config.getLogging();

		if (logging != null) {
			for (AppenderConfigurationType appender : logging.getAppender()) {
				if (appender instanceof FileAppenderConfigurationType) {
					appenders.add(new FileAppenderConfig((FileAppenderConfigurationType) appender));
				} else {
					appenders.add(new AppenderConfiguration(appender));
				}
			}
			Collections.sort(appenders);
			loggingConfig = new LoggingDto(config.getLogging());
		} else {
			loggingConfig = new LoggingDto();
		}
		loggingConfig.setAppenders(appenders);

		// PROFILING
		if (config.getProfilingConfiguration() != null) {
			List<ClassLoggerConfigurationType> classLoggerConfig = config.getLogging() != null
					? config.getLogging().getClassLogger() : null;
			profilingDto = new ProfilingDto(config.getProfilingConfiguration(), classLoggerConfig);
		} else {
			profilingDto = new ProfilingDto();
		}
		profilingDto.setAppenders(appenders);

		enableExperimentalCode = SystemConfigurationTypeUtil.isExperimentalCodeEnabled(config);

		userDashboardLink = loadUserDashboardLink(config);
        additionalMenuLink = loadAdditionalMenuItem(config);
	}

	public SystemConfigurationType getOldObject() {
		return oldObject;
	}

	public SystemConfigurationType getNewObject() throws DatatypeConfigurationException {
		SystemConfigurationType newObject = oldObject.clone();
		if (StringUtils.isNotBlank(getPassPolicyDto().getOid())) {
			ObjectReferenceType globalPassPolicyRef = ObjectTypeUtil
					.createObjectRef(getPassPolicyDto().getOid(), ObjectTypes.PASSWORD_POLICY);
			newObject.setGlobalPasswordPolicyRef(globalPassPolicyRef);
		} else {
			newObject.setGlobalPasswordPolicyRef(null);
		}

		if (StringUtils.isNotBlank(getSecurityPolicyDto().getOid())) {
			ObjectReferenceType globalSecurityPolicyRef = ObjectTypeUtil.createObjectRef(
					getSecurityPolicyDto().getOid(),
					WebComponentUtil.createPolyFromOrigString(getSecurityPolicyDto().getName()),
					ObjectTypes.SECURITY_POLICY);
			newObject.setGlobalSecurityPolicyRef(globalSecurityPolicyRef);
		} else {
			newObject.setGlobalSecurityPolicyRef(null);
		}

		AssignmentPolicyEnforcementType globalAEP = AEPlevel.toAEPValueType(getAepLevel());
		if (globalAEP != null) {
			ProjectionPolicyType projectionPolicy = new ProjectionPolicyType();
			projectionPolicy.setAssignmentPolicyEnforcement(globalAEP);
			newObject.setGlobalAccountSynchronizationSettings(projectionPolicy);
		}
		CleanupPoliciesType cleanupPolicies = new CleanupPoliciesType();
		cleanupPolicies.setAuditRecords(auditCleanup.toConfig());
		cleanupPolicies.setClosedTasks(taskCleanup.toConfig());
		cleanupPolicies.setClosedCertificationCampaigns(campaignsCleanup.toConfig());
		cleanupPolicies.setOutputReports(reportsCleanup.toConfig());
		cleanupPolicies.setObjectResults(resultsCleanup.toConfig());

		DeploymentInformationType deploymentInformation = getDeploymentInformation();
		newObject.setDeploymentInformation(deploymentInformation);

		newObject.setCleanupPolicy(cleanupPolicies);
		SystemConfigurationTypeUtil.setEnableExperimentalCode(newObject, getEnableExperimentalCode());

		newObject.setLogging(loggingConfig.getNewObject());

		newObject.setNotificationConfiguration(notificationConfig.getNewObject(newObject));

		newObject.setProfilingConfiguration(profilingDto.getNewObject());
		ClassLoggerConfigurationType profilingClassLogger = profilingDto.getProfilingClassLogerConfig();
		if (newObject.getLogging() != null) {
			newObject.getLogging().getClassLogger().add(profilingClassLogger);
		} else {
			LoggingConfigurationType profLogging = new LoggingConfigurationType();
			profLogging.getClassLogger().add(profilingClassLogger);
			newObject.setLogging(profLogging);
		}

		return newObject;
	}

	public static List<RichHyperlinkType> loadUserDashboardLink(SystemConfigurationType config) {
		List<RichHyperlinkType> links = new ArrayList<>();
		if (config == null || config.getInternals() == null
				|| config.getInternals().isEnableExperimentalCode() == null) {
			return links;
		}
		if (config.getAdminGuiConfiguration() != null) {
			links.addAll(config.getAdminGuiConfiguration().getUserDashboardLink());
		}
		return links;
	}

	public static List<RichHyperlinkType> loadAdditionalMenuItem(SystemConfigurationType config) {
		List<RichHyperlinkType> links = new ArrayList<>();
		if (config == null || config.getInternals() == null
				|| config.getInternals().isEnableExperimentalCode() == null) {
			return links;
		}
		if (config.getAdminGuiConfiguration() != null) {
			links.addAll(config.getAdminGuiConfiguration().getAdditionalMenuLink());
		}
		return links;
	}

	private ObjectViewDto<ValuePolicyType> loadPasswordPolicy(SystemConfigurationType config) {
		ValuePolicyType passPolicy = config.getGlobalPasswordPolicy();

		if (passPolicy != null) {
			passPolicyDto = new ObjectViewDto<>(passPolicy.getOid(), passPolicy.getName().getOrig());
		} else {
			passPolicyDto = new ObjectViewDto<>();
		}

		passPolicyDto.setType(ValuePolicyType.class);
		return passPolicyDto;
	}

	private ObjectViewDto<SecurityPolicyType> loadSecurityPolicy(SystemConfigurationType config) {
		ObjectReferenceType securityPolicy = config.getGlobalSecurityPolicyRef();

		if (securityPolicy != null) {
			securityPolicyDto = new ObjectViewDto<>(securityPolicy.getOid(),
                WebComponentUtil.getName(securityPolicy));
		} else {
			securityPolicyDto = new ObjectViewDto<>();
		}

		securityPolicyDto.setType(SecurityPolicyType.class);
		return securityPolicyDto;
	}

	public DeploymentInformationType getDeploymentInformation() { return deploymentInformation; }

	public void setDeploymentInformation(DeploymentInformationType deploymentInformation) { this.deploymentInformation = deploymentInformation; }

	public String getAuditCleanupAge() {
		return auditCleanup.ageValue;
	}

	public void setAuditCleanupAge(String value) {
		auditCleanup.ageValue = value;
	}

	public Integer getAuditCleanupRecords() {
		return auditCleanup.records;
	}

	public void setAuditCleanupRecords(Integer value) {
		auditCleanup.records = value;
	}

	public String getTaskCleanupAge() {
		return taskCleanup.ageValue;
	}

	public void setTaskCleanupAge(String value) {
		taskCleanup.ageValue = value;
	}

	public Integer getTaskCleanupRecords() {
		return taskCleanup.records;
	}

	public void setTaskCleanupRecords(Integer value) {
		taskCleanup.records = value;
	}
	
	public String getCampaignCleanupAge() {
		return campaignsCleanup.ageValue;
	}

	public void setCampaignCleanupAge(String value) {
		campaignsCleanup.ageValue = value;
	}

	public Integer getCampaignCleanupRecords() {
		return campaignsCleanup.records;
	}

	public void setCampaignCleanupRecords(Integer value) {
		campaignsCleanup.records = value;
	}

	public String getReportCleanupAge() {
		return reportsCleanup.ageValue;
	}

	public void setReportCleanupAge(String value) {
		reportsCleanup.ageValue = value;
	}

	public Integer getReportCleanupRecords() {
		return reportsCleanup.records;
	}

	public void setReportCleanupRecords(Integer value) {
		reportsCleanup.records = value;
	}

	public String getResultCleanupAge() {
		return resultsCleanup.ageValue;
	}

	public void setResultCleanupAge(String value) {
		resultsCleanup.ageValue = value;
	}

	public Integer getResultCleanupRecords() {
		return resultsCleanup.records;
	}

	public void setResultCleanupRecords(Integer value) {
		resultsCleanup.records = value;
	}

	public AEPlevel getAepLevel() {
		return aepLevel;
	}

	public void setAepLevel(AEPlevel aepLevel) {
		this.aepLevel = aepLevel;
	}

	public ObjectViewDto<ValuePolicyType> getPassPolicyDto() {
		return passPolicyDto;
	}

	public void setPassPolicyDto(ObjectViewDto<ValuePolicyType> passPolicyDto) {
		this.passPolicyDto = passPolicyDto;
	}

	public ObjectViewDto<SecurityPolicyType> getSecurityPolicyDto() {
		return securityPolicyDto;
	}

	public void setSecurityPolicyDto(ObjectViewDto<SecurityPolicyType> securityPolicyDto) {
		this.securityPolicyDto = securityPolicyDto;
	}

	public NotificationConfigurationDto getNotificationConfig() {
		return notificationConfig;
	}

	public void setNotificationConfig(NotificationConfigurationDto notificationConfig) {
		this.notificationConfig = notificationConfig;
	}

	public LoggingDto getLoggingConfig() {
		return loggingConfig;
	}

	public void setLoggingConfig(LoggingDto loggingConfig) {
		this.loggingConfig = loggingConfig;
	}

	public ProfilingDto getProfilingDto() {
		return profilingDto;
	}

	public void setProfilingDto(ProfilingDto profilingDto) {
		this.profilingDto = profilingDto;
	}

	public List<ObjectPolicyConfigurationTypeDto> getObjectPolicyList() {
		return objectPolicyList;
	}

	public void setObjectPolicyList(List<ObjectPolicyConfigurationTypeDto> objectPolicyList) {
		this.objectPolicyList = objectPolicyList;
	}

	public Boolean getEnableExperimentalCode() {
		return enableExperimentalCode;
	}

	public void setEnableExperimentalCode(Boolean enableExperimentalCode) {
		this.enableExperimentalCode = enableExperimentalCode;
	}

	public List<RichHyperlinkType> getUserDashboardLink() {
		return userDashboardLink;
	}

	public void setUserDashboardLink(List<RichHyperlinkType> userDashboardLink) {
		this.userDashboardLink = userDashboardLink;
	}

    public List<RichHyperlinkType> getAdditionalMenuLink() {
        return additionalMenuLink;
    }

    public void setAdditionalMenuLink(List<RichHyperlinkType> additionalMenuLink) {
        this.additionalMenuLink = additionalMenuLink;
    }
}
