/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.LoggingConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.NotificationConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.ProfilingConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.SystemConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AEPlevel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AppenderConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ClassLogger;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ComponentLogger;
import com.evolveum.midpoint.web.page.admin.configuration.dto.FilterConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggerConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggingDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.MailServerConfigurationTypeDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.NotificationConfigurationDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectPolicyConfigurationTypeDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingLevel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.PropertyConstraintTypeDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.StandardLogger;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProfilingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubSystemLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lazyman
 */
@PageDescriptor(url = { "/admin/config", "/admin/config/system" }, action = {
		@AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL, label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL, label = "PageSystemConfiguration.auth.configSystemConfiguration.label", description = "PageSystemConfiguration.auth.configSystemConfiguration.description") })
public class PageSystemConfiguration extends PageAdminConfiguration {

	public static final String SELECTED_TAB_INDEX = "tab";

	public static final int CONFIGURATION_TAB_BASIC = 0;
	public static final int CONFIGURATION_TAB_NOTIFICATION = 1;
	public static final int CONFIGURATION_TAB_LOGGING = 2;
	public static final int CONFIGURATION_TAB_PROFILING = 3;

	private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfiguration.class);

	private static final String DOT_CLASS = PageSystemConfiguration.class.getName() + ".";
	private static final String TASK_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";
	private static final String TASK_UPDATE_SYSTEM_CONFIG = DOT_CLASS + "updateSystemConfiguration";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TAB_PANEL = "tabPanel";
	private static final String ID_CANCEL = "cancel";
	private static final String ID_SAVE = "save";

	public static final String ROOT_APPENDER_INHERITANCE_CHOICE = "(Inherit root)";

	private LoggingConfigPanel loggingConfigPanel;
	private ProfilingConfigPanel profilingConfigPanel;
	private SystemConfigPanel systemConfigPanel;
	private NotificationConfigPanel notificationConfigPanel;

	private LoadableModel<SystemConfigurationDto> model;

	private boolean initialized;

	public PageSystemConfiguration() {
		this(null);
	}

	public PageSystemConfiguration(PageParameters parameters) {
		super(parameters);

		model = new LoadableModel<SystemConfigurationDto>(false) {

			@Override
			protected SystemConfigurationDto load() {
				return loadSystemConfiguration();
			}
		};

		initLayout();
	}

	private SystemConfigurationDto loadSystemConfiguration() {
		Task task = createSimpleTask(TASK_GET_SYSTEM_CONFIG);
		OperationResult result = new OperationResult(TASK_GET_SYSTEM_CONFIG);

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				GetOperationOptions.createResolve(), SystemConfigurationType.F_DEFAULT_USER_TEMPLATE,
				SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY);

		SystemConfigurationDto dto = null;
		try {
			PrismObject<SystemConfigurationType> systemConfig = WebModelUtils.loadObject(
					SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), options,
					this, task, result);
			dto = new SystemConfigurationDto(systemConfig);
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't load system configuration", ex);
			result.recordFatalError("Couldn't load system configuration.", ex);
		}

		// what do you do with null? many components depends on this not to be
		// null :)
		if (!WebMiscUtil.isSuccessOrHandledError(result) || dto == null) {
			showResultInSession(result);
			throw getRestartResponseException(PageError.class);
		}

		return dto;
	}

	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM, true);
		add(mainForm);

		List<ITab> tabs = new ArrayList<>();
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.system.title")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				systemConfigPanel = new SystemConfigPanel(panelId, model);
				return systemConfigPanel;
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.notifications.title")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				notificationConfigPanel = new NotificationConfigPanel(panelId,
						new PropertyModel<NotificationConfigurationDto>(model, "notificationConfig"));
				return notificationConfigPanel;
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.logging.title")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				loggingConfigPanel = new LoggingConfigPanel(panelId,
						new PropertyModel<LoggingDto>(model, "loggingConfig"));
				return loggingConfigPanel;
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.profiling.title")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				profilingConfigPanel = new ProfilingConfigPanel(panelId,
						new PropertyModel<ProfilingDto>(model, "profilingDto"));
				return profilingConfigPanel;
			}
		});

		TabbedPanel tabPanel = new TabbedPanel(ID_TAB_PANEL, tabs);
		tabPanel.setOutputMarkupId(true);
		mainForm.add(tabPanel);

		initButtons(mainForm);
	}

	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();

		if (!initialized) {
			PageParameters params = getPageParameters();
			StringValue val = params.get(SELECTED_TAB_INDEX);
			String value = null;
			if (val != null && !val.isNull()) {
				value = val.toString();
			}

			int index = StringUtils.isNumeric(value) ? Integer.parseInt(value) : CONFIGURATION_TAB_BASIC;
			getTabPanel().setSelectedTab(index);

			initialized = true;
		}
	}

	private void initButtons(Form mainForm) {
		AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(save);

		AjaxButton cancel = new AjaxButton(ID_CANCEL, createStringResource("PageBase.button.cancel")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		mainForm.add(cancel);
	}


	private TabbedPanel getTabPanel() {
		return (TabbedPanel) get(createComponentPath(ID_MAIN_FORM, ID_TAB_PANEL));
	}

//	private ClassLoggerConfigurationType createCustomClassLogger(String name, LoggingLevelType level,
//			String appender) {
//		ClassLoggerConfigurationType type = new ClassLoggerConfigurationType();
//		type.setPackage(name);
//		type.setLevel(level);
//		if (StringUtils.isNotEmpty(appender) || !(ROOT_APPENDER_INHERITANCE_CHOICE.equals(appender))) {
//			type.getAppender().add(appender);
//		}
//
//		return type;
//	}

//	private ProfilingConfigurationType createProfilingConfiguration(ProfilingDto dto) {
//		ProfilingConfigurationType config = new ProfilingConfigurationType();
//
//		if (dto.isPerformanceStatistics() || dto.isRequestFilter() || dto.isSubsystemModel()
//				|| dto.isSubsystemRepository() || dto.isSubsystemProvisioning()
//				|| dto.isSubsystemResourceObjectChangeListener() || dto.isSubsystemUcf()
//				|| dto.isSubsystemTaskManager() || dto.isSubsystemWorkflow())
//			config.setEnabled(true);
//		else
//			config.setEnabled(false);
//
//		LOGGER.info("Profiling enabled: " + config.isEnabled());
//
//		config.setDumpInterval(dto.getDumpInterval());
//		config.setPerformanceStatistics(dto.isPerformanceStatistics());
//		config.setRequestFilter(dto.isRequestFilter());
//		config.setModel(dto.isSubsystemModel());
//		config.setProvisioning(dto.isSubsystemProvisioning());
//		config.setRepository(dto.isSubsystemRepository());
//		config.setUcf(dto.isSubsystemUcf());
//		config.setResourceObjectChangeListener(dto.isSubsystemResourceObjectChangeListener());
//		config.setTaskManager(dto.isSubsystemTaskManager());
//		config.setWorkflow(dto.isSubsystemWorkflow());
//
//		return config;
//	}

	private void savePerformed(AjaxRequestTarget target) {
		OperationResult result = new OperationResult(TASK_UPDATE_SYSTEM_CONFIG);
		String oid = SystemObjectsType.SYSTEM_CONFIGURATION.value();
		Task task = createSimpleTask(TASK_UPDATE_SYSTEM_CONFIG);
		try {


			SystemConfigurationType newObject = model.getObject().getNewObject();
			
//			newObject = saveLogging(target, newObject);
//			newObject = saveNotificationConfiguration(newObject);
//			newObject = saveProfiling(newObject);
			saveObjectPolicies(newObject);

//			if (LOGGER.isTraceEnabled())
//				LOGGER.trace("Saving logging configuration.");
//
//			if (StringUtils.isEmpty(globalPasswordPolicyOid)) {
//				s.setGlobalPasswordPolicyRef(null);
//			} else {
//				s.setGlobalPasswordPolicyRef(globalPassPolicyRef);
//			}
//
//			if (StringUtils.isEmpty(globalObjectTemplateOid)) {
//				s.setDefaultUserTemplateRef(null);
//			} else {
//				s.setDefaultUserTemplateRef(globalObjectTemplateRef);
//			}

//			s.setGlobalAccountSynchronizationSettings(projectionPolicy);
//			s.setCleanupPolicy(cleanupPolicies);
//			SystemConfigurationTypeUtil.setEnableExperimentalCode(s, dto.getEnableExperimentalCode());
//
//			PrismObject<SystemConfigurationType> oldObject = getModelService()
//					.getObject(SystemConfigurationType.class, oid, null, task, result);

//			newObject = s.asPrismObject();

			ObjectDelta<SystemConfigurationType> delta = DiffUtil.diff(model.getObject().getOldObject().asPrismObject(), newObject.asPrismObject());
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("System configuration delta:\n{}", delta.debugDump());
			}
			if (delta != null && !delta.isEmpty()) {
				getPrismContext().adopt(delta);
				getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task,
						result);
			}

			result.computeStatusIfUnknown();
		} catch (Exception e) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't save system configuration.", e);
			LoggingUtils.logException(LOGGER, "Couldn't save system configuration.", e);
		}

		showResultInSession(result);
		target.add(getFeedbackPanel());
		resetPerformed(target);
	}

	private void saveObjectPolicies(SystemConfigurationType systemConfig) {
		if (systemConfigPanel == null) {
			return;
		}

		List<ObjectPolicyConfigurationTypeDto> configList = systemConfigPanel.getModel().getObject()
				.getObjectPolicyList();
		List<ObjectPolicyConfigurationType> confList = new ArrayList<>();

		ObjectPolicyConfigurationType newObjectPolicyConfig;
		for (ObjectPolicyConfigurationTypeDto o : configList) {
			newObjectPolicyConfig = new ObjectPolicyConfigurationType();
			newObjectPolicyConfig.setType(o.getType());
			newObjectPolicyConfig.setObjectTemplateRef(o.getTemplateRef());

			List<PropertyConstraintType> constraintList = new ArrayList<>();
			PropertyConstraintType property;

			if (o.getConstraints() != null) {
				for (PropertyConstraintTypeDto c : o.getConstraints()) {
					if (StringUtils.isNotEmpty(c.getPropertyPath())) {
						property = new PropertyConstraintType();
						property.setOidBound(c.isOidBound());
						property.setPath(new ItemPathType(c.getPropertyPath()));

						constraintList.add(property);
					}
				}
			}

			newObjectPolicyConfig.getPropertyConstraint().addAll(constraintList);

			confList.add(newObjectPolicyConfig);
		}

		systemConfig.getDefaultObjectPolicyConfiguration().clear();
		systemConfig.getDefaultObjectPolicyConfiguration().addAll(confList);
	}

//	private SystemConfigurationType saveNotificationConfiguration(SystemConfigurationType config) {
//		NotificationConfigurationDto dto;
//		NotificationConfigurationType notificationConfig;
//		MailConfigurationType mailConfig;
//
//		if (notificationConfigPanel != null && notificationConfigPanel.getModel().getObject() != null) {
//			dto = notificationConfigPanel.getModel().getObject();
//
//			if (config.getNotificationConfiguration() != null) {
//				notificationConfig = config.getNotificationConfiguration();
//			} else {
//				notificationConfig = new NotificationConfigurationType();
//			}
//
//			if (notificationConfig.getMail() != null) {
//				mailConfig = notificationConfig.getMail();
//			} else {
//				mailConfig = new MailConfigurationType();
//			}
//
//			mailConfig.setDebug(dto.isDebug());
//			mailConfig.setDefaultFrom(dto.getDefaultFrom());
//			mailConfig.setRedirectToFile(dto.getRedirectToFile());
//
//			mailConfig.getServer().clear();
//			for (MailServerConfigurationTypeDto serverDto : dto.getServers()) {
//				MailServerConfigurationType newConfig = new MailServerConfigurationType();
//				newConfig.setHost(serverDto.getHost());
//				newConfig.setPort(serverDto.getPort());
//				newConfig.setUsername(serverDto.getUsername());
//				newConfig.setTransportSecurity(serverDto.getMailTransportSecurityType());
//
//				if (serverDto.getPassword() != null && StringUtils.isNotEmpty(serverDto.getPassword())) {
//					ProtectedStringType pass = new ProtectedStringType();
//					pass.setClearValue(serverDto.getPassword());
//					newConfig.setPassword(pass);
//				} else {
//					newConfig.setPassword(serverDto.getOldConfig().getPassword());
//				}
//
//				mailConfig.getServer().add(newConfig);
//			}
//
//			notificationConfig.setMail(mailConfig);
//			config.setNotificationConfiguration(notificationConfig);
//		}
//
//		return config;
//	}

//	private SystemConfigurationType saveLogging(AjaxRequestTarget target, SystemConfigurationType config) {
//		LoggingDto loggingDto = null;
//		LoggingConfigurationType loggingConfig = null;
//
//		if (loggingConfigPanel != null) {
//			loggingDto = loggingConfigPanel.getModel().getObject();
//			loggingConfig = createLoggingConfiguration(loggingDto);
//
//			if (loggingConfig == null) {
//				target.add(getFeedbackPanel());
//				target.add(get(ID_MAIN_FORM));
//				return config;
//			}
//
//			// profilingConfig = createProfilingConfiguration(loggingDto);
//			// if(profilingConfig == null){
//			// target.add(getFeedbackPanel());
//			// target.add(get(ID_MAIN_FORM));
//			// return config;
//			// }
//		}
//
//		if (loggingConfigPanel != null) {
//			config.setLogging(loggingConfig);
//			// config.setProfilingConfiguration(profilingConfig);
//		}
//
//		if (loggingConfigPanel != null) {
//			for (LoggerConfiguration logger : loggingDto.getLoggers()) {
//				logger.setEditing(false);
//			}
//			for (FilterConfiguration filter : loggingDto.getFilters()) {
//				filter.setEditing(false);
//			}
//			for (AppenderConfiguration appender : loggingDto.getAppenders()) {
//				appender.setEditing(false);
//			}
//		}
//
//		return config;
//	}

//	private SystemConfigurationType saveProfiling(AjaxRequestTarget target, SystemConfigurationType config) {
//		ProfilingDto loggingDto = null;
//		ProfilingConfigurationType profilingConfig = null;
//
//		if (profilingConfigPanel != null) {
//			loggingDto = profilingConfigPanel.getModel().getObject();
//
//			if (loggingDto != null) {
//				profilingConfig = createProfilingConfiguration(loggingDto);
//			}
//			if (profilingConfig == null) {
//				target.add(getFeedbackPanel());
//				target.add(get(ID_MAIN_FORM));
//				return config;
//			}
//		}
//
//		if (profilingConfigPanel != null) {
//			// config.setLogging(loggingConfig);
//			config.setProfilingConfiguration(profilingConfig);
//			if (loggingDto.getProfilingLevel() != null) {
//				ClassLoggerConfigurationType type = createCustomClassLogger(ProfilingDto.LOGGER_PROFILING,
//						ProfilingLevel.toLoggerLevelType(loggingDto.getProfilingLevel()),
//						loggingDto.getProfilingAppender());
//				LoggingConfigurationType loggingConfig = config.getLogging();
//				if (loggingConfig == null) {
//					loggingConfig = new LoggingConfigurationType();
//				}
//				loggingConfig.getClassLogger().add(type);
//			}
//		}
//
//		return config;
//	}

	private void resetPerformed(AjaxRequestTarget target) {
		int index = getTabPanel().getSelectedTab();

		PageParameters params = new PageParameters();
		params.add(SELECTED_TAB_INDEX, index);
		PageSystemConfiguration page = new PageSystemConfiguration(params);
		setResponsePage(page);
	}

	private void cancelPerformed(AjaxRequestTarget target) {
		resetPerformed(target);
	}
}
