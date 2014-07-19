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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.LoggingConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.SystemConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.*;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = {"/admin/config", "/admin/config/system"}, action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#configSystemConfiguration",
                label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                description = "PageSystemConfiguration.auth.configSystemConfiguration.description")})
public class PageSystemConfiguration extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfiguration.class);

    private static final String DOT_CLASS = PageSystemConfiguration.class.getName() + ".";
    private static final String TASK_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";
    private static final String TASK_UPDATE_SYSTEM_CONFIG = DOT_CLASS + "updateSystemConfiguration";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_CANCEL = "cancel";
    private static final String ID_SAVE = "save";

    public static final String ROOT_APPENDER_INHERITANCE_CHOICE = "(Inherit root)";

    LoggingConfigPanel loggingConfigPanel;
    SystemConfigPanel systemConfigPanel;

    private LoadableModel<SystemConfigurationDto> model;

    public PageSystemConfiguration() {

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

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolve(),
                        SystemConfigurationType.F_DEFAULT_USER_TEMPLATE ,SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY);

        SystemConfigurationDto dto = null;
        try{
            PrismObject<SystemConfigurationType> systemConfig = getModelService().getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), options, task, result);

            dto = new SystemConfigurationDto(systemConfig, getMidpointApplication().getProtector());
            result.recordSuccess();
        } catch(Exception ex){
            LoggingUtils.logException(LOGGER, "Couldn't load system configuration", ex);
            result.recordFatalError("Couldn't load system configuration.", ex);
        }

        //what do you do with null? many components depends on this not to be null :)
        if(!WebMiscUtil.isSuccessOrHandledError(result) || dto == null) {
            showResultInSession(result);
            throw getRestartResponseException(PageError.class);
        }

        return dto;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.system.title")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                systemConfigPanel = new SystemConfigPanel(panelId, model);
                return  systemConfigPanel;
            }
        });
        tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.logging.title")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                loggingConfigPanel = new LoggingConfigPanel(panelId);
                return loggingConfigPanel;
            }
        });

        mainForm.add(new TabbedPanel(ID_TAB_PANEL, tabs));

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
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

    private LoggingConfigurationType createLoggingConfiguration(LoggingDto dto) {
        LoggingConfigurationType configuration = new LoggingConfigurationType();
        AuditingConfigurationType audit = new AuditingConfigurationType();
        audit.setEnabled(dto.isAuditLog());
        audit.setDetails(dto.isAuditDetails());
        if (StringUtils.isNotEmpty(dto.getAuditAppender())) {
            audit.getAppender().add(dto.getAuditAppender());
        }
        configuration.setAuditing(audit);
        configuration.setRootLoggerAppender(dto.getRootAppender());
        configuration.setRootLoggerLevel(dto.getRootLevel());

        for (AppenderConfiguration item : dto.getAppenders()) {
            configuration.getAppender().add(item.getConfig());
        }

        for (LoggerConfiguration item : dto.getLoggers()) {
            if (LoggingDto.LOGGER_PROFILING.equals(item.getName())) {
                continue;
            }

            for(ClassLoggerConfigurationType logger : configuration.getClassLogger()){
                if(logger.getPackage().equals(item.getName())){
                    error("Logger with name '" + item.getName() + "' is already defined.");
                    return null;
                }
            }

            if (item instanceof StandardLogger){
                configuration.getClassLogger().add(((StandardLogger)item).toXmlType());
            } else if (item instanceof ComponentLogger){
                configuration.getClassLogger().add(((ComponentLogger) item).toXmlType());
            } else {
                configuration.getClassLogger().add(((ClassLogger) item).toXmlType());
            }

        }

        for (FilterConfiguration item : dto.getFilters()) {
            if (LoggingDto.LOGGER_PROFILING.equals(item.getName())) {
                continue;
            }

            for(SubSystemLoggerConfigurationType  filter : configuration.getSubSystemLogger()){
                if(filter.getComponent().name().equals(item.getName())){
                    error("Filter with name '" + item.getName() + "' is already defined.");
                    return null;
                }
            }

            configuration.getSubSystemLogger().add(item.toXmlType());
        }

        if (dto.getProfilingLevel() != null) {
            ClassLoggerConfigurationType type = createCustomClassLogger(LoggingDto.LOGGER_PROFILING,
                    ProfilingLevel.toLoggerLevelType(dto.getProfilingLevel()), dto.getProfilingAppender());
            configuration.getClassLogger().add(type);
        }

        return configuration;
    }

    private ClassLoggerConfigurationType createCustomClassLogger(String name, LoggingLevelType level, String appender) {
        ClassLoggerConfigurationType type = new ClassLoggerConfigurationType();
        type.setPackage(name);
        type.setLevel(level);
        if (StringUtils.isNotEmpty(appender) || !(ROOT_APPENDER_INHERITANCE_CHOICE.equals(appender))) {
            type.getAppender().add(appender);
        }

        return type;
    }

    private ProfilingConfigurationType createProfilingConfiguration(LoggingDto dto){
        ProfilingConfigurationType config = new ProfilingConfigurationType();

        if(dto.isPerformanceStatistics() || dto.isRequestFilter() || dto.isSubsystemModel() || dto.isSubsystemRepository() || dto.isSubsystemProvisioning()
                || dto.isSubsystemResourceObjectChangeListener() || dto.isSubsystemUcf() || dto.isSubsystemTaskManager() || dto.isSubsystemWorkflow())
            config.setEnabled(true);
        else
            config.setEnabled(false);

        LOGGER.info("Profiling enabled: " + config.isEnabled());

        config.setDumpInterval(dto.getDumpInterval());
        config.setPerformanceStatistics(dto.isPerformanceStatistics());
        config.setRequestFilter(dto.isRequestFilter());
        config.setModel(dto.isSubsystemModel());
        config.setProvisioning(dto.isSubsystemProvisioning());
        config.setRepository(dto.isSubsystemRepository());
        config.setUcf(dto.isSubsystemUcf());
        config.setResourceObjectChangeListener(dto.isSubsystemResourceObjectChangeListener());
        config.setTaskManager(dto.isSubsystemTaskManager());
        config.setWorkflow(dto.isSubsystemWorkflow());

        return config;
    }

    //TODO - save the rest of systemConfig
    private void savePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(TASK_UPDATE_SYSTEM_CONFIG);
        String oid = SystemObjectsType.SYSTEM_CONFIGURATION.value();

        try{
            SystemConfigurationDto dto = model.getObject();

            String globalPasswordPolicyOid = dto.getPassPolicyDto().getOid();
            ObjectReferenceType globalPassPolicyRef = new ObjectReferenceType();
            globalPassPolicyRef.setOid(globalPasswordPolicyOid);

            String globalObjectTemplateOid = dto.getObjectTemplateDto().getOid();
            ObjectReferenceType globalObjectTemplateRef = new ObjectReferenceType();
            globalObjectTemplateRef.setOid(globalObjectTemplateOid);

            AssignmentPolicyEnforcementType globalAEP = AEPlevel.toAEPValueType(dto.getAepLevel());
            ProjectionPolicyType projectionPolicy = new ProjectionPolicyType();
            projectionPolicy.setAssignmentPolicyEnforcement(globalAEP);

            Duration auditCleanupDuration = DatatypeFactory.newInstance().newDuration(dto.getAuditCleanupValue());
            Duration cleanupTaskDuration = DatatypeFactory.newInstance().newDuration(dto.getTaskCleanupValue());
            CleanupPolicyType auditCleanup = new CleanupPolicyType();
            CleanupPolicyType taskCleanup = new CleanupPolicyType();
            auditCleanup.setMaxAge(auditCleanupDuration);
            taskCleanup.setMaxAge(cleanupTaskDuration);
            CleanupPoliciesType cleanupPolicies = new CleanupPoliciesType();
            cleanupPolicies.setAuditRecords(auditCleanup);
            cleanupPolicies.setClosedTasks(taskCleanup);

            Task task = createSimpleTask(TASK_UPDATE_SYSTEM_CONFIG);
            PrismObject<SystemConfigurationType> newObject = getModelService().getObject(SystemConfigurationType.class,
                    oid, null, task, result);
            SystemConfigurationType s = newObject.asObjectable();

            s = saveLogging(target, s);
            s = saveNotificationConfiguration(s);

            if(LOGGER.isTraceEnabled())
                LOGGER.trace("Saving logging configuration.");

            if(StringUtils.isEmpty(globalPasswordPolicyOid)){
                s.setGlobalPasswordPolicyRef(null);
            }else{
                s.setGlobalPasswordPolicyRef(globalPassPolicyRef);
            }

            if(StringUtils.isEmpty(globalObjectTemplateOid)){
                s.setDefaultUserTemplateRef(null);
            }else{
                s.setDefaultUserTemplateRef(globalObjectTemplateRef);
            }

            s.setGlobalAccountSynchronizationSettings(projectionPolicy);
            s.setCleanupPolicy(cleanupPolicies);

            PrismObject<SystemConfigurationType> oldObject = getModelService().getObject(SystemConfigurationType.class,
                    oid, null, task, result);

            newObject = s.asPrismObject();

            ObjectDelta<SystemConfigurationType> delta = DiffUtil.diff(oldObject, newObject);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("System configuration delta:\n{}", delta.debugDump());
            }
            if (delta != null && !delta.isEmpty()){
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            }

            result.computeStatusIfUnknown();
        } catch (Exception e){
            result.recomputeStatus();
            result.recordFatalError("Couldn't save system configuration.", e);
            LoggingUtils.logException(LOGGER,"Couldn't save system configuration.", e);
        }

        showResultInSession(result);
        target.add(getFeedbackPanel());
        resetPerformed(target);
    }

    /*
    *   TODO - currently, we are saving changes to MailServerConfigType on index 0 in ArrayList. This will
    *   change when GUI is update to define multiple mailServer or even SMS notifications
    * */
    private SystemConfigurationType saveNotificationConfiguration(SystemConfigurationType config){
        NotificationConfigurationDto dto;
        NotificationConfigurationType notificationConfig;
        MailConfigurationType mailConfig;
        MailServerConfigurationType mailServerConfig;

        if(systemConfigPanel.getModel().getObject().getNotificationConfig() != null){
            dto = systemConfigPanel.getModel().getObject().getNotificationConfig();

            if(config.getNotificationConfiguration() != null){
                notificationConfig = config.getNotificationConfiguration();
            } else {
                notificationConfig = new NotificationConfigurationType();
            }

            if(notificationConfig.getMail() != null){
                mailConfig = notificationConfig.getMail();
            } else {
                mailConfig = new MailConfigurationType();
            }

            mailConfig.setDebug(dto.isDebug());
            mailConfig.setDefaultFrom(dto.getDefaultFrom());
            mailConfig.setRedirectToFile(dto.getRedirectToFile());

            if(!mailConfig.getServer().isEmpty() && mailConfig.getServer().get(0) != null){
                mailServerConfig = mailConfig.getServer().get(0);
            } else {
                mailServerConfig = new MailServerConfigurationType();
            }

            mailServerConfig.setHost(dto.getHost());
            mailServerConfig.setPort(dto.getPort());
            mailServerConfig.setUsername(dto.getUsername());
            mailServerConfig.setTransportSecurity(dto.getMailTransportSecurityType());

            ProtectedStringType pass = new ProtectedStringType();
            pass.setClearValue(dto.getPassword());
            mailServerConfig.setPassword(pass);

            if(mailConfig.getServer().isEmpty()){
                if(dto.isConfigured())
                    mailConfig.getServer().add(0, mailServerConfig);
            } else {
                if(dto.isConfigured())
                    mailConfig.getServer().set(0, mailServerConfig);
                else
                    mailConfig.getServer().remove(0);
            }

            notificationConfig.setMail(mailConfig);
            config.setNotificationConfiguration(notificationConfig);
        }

        return config;
    }

    private SystemConfigurationType saveLogging(AjaxRequestTarget target, SystemConfigurationType config){
        LoggingDto loggingDto = null;
        LoggingConfigurationType loggingConfig = null;
        ProfilingConfigurationType profilingConfig = null;

        if(loggingConfigPanel != null){
            loggingDto = loggingConfigPanel.getModel().getObject();
            loggingConfig = createLoggingConfiguration(loggingDto);

            if(loggingConfig == null){
                target.add(getFeedbackPanel());
                target.add(get(ID_MAIN_FORM));
                return config;
            }

            profilingConfig = createProfilingConfiguration(loggingDto);
            if(profilingConfig == null){
                target.add(getFeedbackPanel());
                target.add(get(ID_MAIN_FORM));
                return config;
            }
        }

        if(loggingConfigPanel != null){
            config.setLogging(loggingConfig);
            config.setProfilingConfiguration(profilingConfig);
        }

        if(loggingConfigPanel != null){
            for (LoggerConfiguration logger : loggingDto.getLoggers()) {
                logger.setEditing(false);
            }
            for (FilterConfiguration filter : loggingDto.getFilters()) {
                filter.setEditing(false);
            }
            for (AppenderConfiguration appender : loggingDto.getAppenders()) {
                appender.setEditing(false);
            }
        }

        return config;
    }

    private void resetPerformed(AjaxRequestTarget target) {
        model.reset();
        setResponsePage(PageSystemConfiguration.class);
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        resetPerformed(target);
    }
}
