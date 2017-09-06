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
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

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
	public static final String F_AUDIT_CLEANUP = "auditCleanupValue";
	public static final String F_TASK_CLEANUP = "taskCleanupValue";
	public static final String F_PASSWORD_POLICY = "passPolicyDto";
	public static final String F_SECURITY_POLICY = "securityPolicyDto";
	public static final String F_OBJECT_TEMPLATE = "objectTemplateDto";
	public static final String F_OBJECT_POLICY_LIST = "objectPolicyList";
	public static final String F_NOTIFICATION_CONFIGURATION = "notificationConfig";
	public static final String F_ENABLE_EXPERIMENTAL_CODE = "enableExperimentalCode";
	public static final String F_USER_DASHBOARD_LINK = "userDashboardLink";
	public static final String F_ADDITIONAL_MENU_LINK = "additionalMenuLink";
	public static final String F_DEPLOYMENT_NAME = "deploymentNameValue";
	public static final String F_DEPLOYMENT_COLOUR = "deploymentColourValue";

	private AEPlevel aepLevel;

	private String auditCleanupValue;
	private String taskCleanupValue;

	private String deploymentNameValue;
	private String deploymentColourValue;

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

		CleanupPolicyType auditCleanup = config.getCleanupPolicy().getAuditRecords();
		CleanupPolicyType taskCleanup = config.getCleanupPolicy().getClosedTasks();

		//DeploymentInformationType deploymentInformation = config.getDeploymentInformation();

		//deploymentNameValue = deploymentInformationdeploymentInformation.getName();
		//deploymentColourValue = deploymentInformation.getHeaderColor();

		auditCleanupValue = auditCleanup.getMaxAge().toString();
		taskCleanupValue = taskCleanup.getMaxAge().toString();

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
		Duration auditCleanupDuration = DatatypeFactory.newInstance().newDuration(getAuditCleanupValue());
		Duration cleanupTaskDuration = DatatypeFactory.newInstance().newDuration(getTaskCleanupValue());
		CleanupPolicyType auditCleanup = new CleanupPolicyType();
		CleanupPolicyType taskCleanup = new CleanupPolicyType();
		auditCleanup.setMaxAge(auditCleanupDuration);
		taskCleanup.setMaxAge(cleanupTaskDuration);
		CleanupPoliciesType cleanupPolicies = new CleanupPoliciesType();
		cleanupPolicies.setAuditRecords(auditCleanup);
		cleanupPolicies.setClosedTasks(taskCleanup);


		//DeploymentInformationType deplymentInformation = new DeploymentInformationType();

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
			securityPolicyDto = new ObjectViewDto<SecurityPolicyType>(securityPolicy.getOid(),
					WebComponentUtil.getName(securityPolicy));
		} else {
			securityPolicyDto = new ObjectViewDto<SecurityPolicyType>();
		}

		securityPolicyDto.setType(SecurityPolicyType.class);
		return securityPolicyDto;
	}

	public String getAuditCleanupValue() {
		return auditCleanupValue;
	}

	public void setAuditCleanupValue(String auditCleanupValue) {
		this.auditCleanupValue = auditCleanupValue;
	}

	public String getTaskCleanupValue() {
		return taskCleanupValue;
	}

	public void setTaskCleanupValue(String taskCleanupValue) {
		this.taskCleanupValue = taskCleanupValue;
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
