/*
 * Copyright (c) 2018 Evolveum
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.GlobalPolicyRuleTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.LoggingConfigurationTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.NotificationConfigTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ObjectPolicyConfigurationTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ProfilingConfigurationTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ContainerOfSystemConfigurationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.SystemConfigPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.SystemConfigurationSummaryPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.progress.ProgressPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;

/**
 * @author lazyman
 * @author skublik
 */
@PageDescriptor(
		urls = {
				@Url(mountUrl = "/admin/config/system", matchUrlForSecurity = "/admin/config/system"),
		},
		action = {
				@AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
						label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
						description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
						label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
						description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
		})
public class PageSystemConfiguration extends PageAdminObjectDetails<SystemConfigurationType> {

	private static final long serialVersionUID = 1L;
	
	public static final String SELECTED_TAB_INDEX = "tab";
	public static final String SELECTED_SERVER_INDEX = "mailServerIndex";
	public static final String SERVER_LIST_SIZE = "mailServerListSize";

	public static final int CONFIGURATION_TAB_BASIC = 0;
	public static final int CONFIGURATION_TAB_OBJECT_POLICY = 1;
	public static final int CONFIGURATION_TAB_GLOBAL_POLICY_RULE = 2;
	public static final int CONFIGURATION_TAB_GLOBAL_ACCOUNT_SYNCHRONIZATION = 3;
	public static final int CONFIGURATION_TAB_CLEANUP_POLICY = 4;
	public static final int CONFIGURATION_TAB_NOTIFICATION = 5;
	public static final int CONFIGURATION_TAB_LOGGING = 6;
	public static final int CONFIGURATION_TAB_PROFILING = 7;
	public static final int CONFIGURATION_TAB_ADMIN_GUI = 8;
	public static final int CONFIGURATION_TAB_WORKFLOW = 9;
	public static final int CONFIGURATION_TAB_ROLE_MANAGEMENT = 10;
	public static final int CONFIGURATION_TAB_INTERNALS = 11;
	public static final int CONFIGURATION_TAB_DEPLOYMENT_INFORMATION = 12;
	public static final int CONFIGURATION_TAB_ACCESS_CERTIFICATION = 13;
	public static final int CONFIGURATION_TAB_INFRASTRUCTURE = 14;
	public static final int CONFIGURATION_TAB_FULL_TEXT_SEARCH = 15;

	private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfiguration.class);
	
	private static final String ID_SUMM_PANEL = "summaryPanel";

	public static final String ROOT_APPENDER_INHERITANCE_CHOICE = "(Inherit root)";

	public PageSystemConfiguration() {
		initialize(null);
	}
	
	public PageSystemConfiguration(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
		initialize(null);
	}
	
	public PageSystemConfiguration(final PrismObject<SystemConfigurationType> configToEdit) {
        initialize(configToEdit);
    }
	
	public PageSystemConfiguration(final PrismObject<SystemConfigurationType> configToEdit, boolean isNewObject)  {
        initialize(configToEdit, isNewObject);
    }
	
	@Override
	protected void initializeModel(final PrismObject<SystemConfigurationType> configToEdit, boolean isNewObject, boolean isReadonly) {
		super.initializeModel(WebModelServiceUtils.loadSystemConfigurationAsObjectWrapper(this).getObject(), false, isReadonly);
    }
	
	private List<ITab> getTabs(){
		List<ITab> tabs = new ArrayList<>();
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.system.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new SystemConfigPanel(panelId, getObjectModel());
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.objectPolicy.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<ObjectPolicyConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION);
				return new ObjectPolicyConfigurationTabPanel(panelId, model);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.globalPolicyRule.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<GlobalPolicyRuleType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						SystemConfigurationType.F_GLOBAL_POLICY_RULE);
				return new GlobalPolicyRuleTabPanel(panelId, model);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.globalAccountSynchronization.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<ProjectionPolicyType>(panelId, getObjectModel(), SystemConfigurationType.F_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.cleanupPolicy.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<CleanupPoliciesType>(panelId, getObjectModel(), SystemConfigurationType.F_CLEANUP_POLICY);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.notifications.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<NotificationConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						SystemConfigurationType.F_NOTIFICATION_CONFIGURATION);
				return new NotificationConfigTabPanel(panelId, model);
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.logging.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<LoggingConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						SystemConfigurationType.F_LOGGING);
				return new LoggingConfigurationTabPanel(panelId, model);
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.profiling.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<ProfilingConfigurationType, SystemConfigurationType> profilingModel = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						SystemConfigurationType.F_PROFILING_CONFIGURATION);
				ContainerWrapperFromObjectWrapperModel<LoggingConfigurationType, SystemConfigurationType> loggingModel = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						SystemConfigurationType.F_LOGGING);
				return new ProfilingConfigurationTabPanel(panelId, profilingModel, loggingModel);
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.adminGui.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<AdminGuiConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.workflow.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<WfConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_WORKFLOW_CONFIGURATION);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.roleManagement.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<RoleManagementConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_ROLE_MANAGEMENT);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.internals.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<InternalsConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_INTERNALS);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.deploymentInformation.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<DeploymentInformationType>(panelId, getObjectModel(), SystemConfigurationType.F_DEPLOYMENT_INFORMATION);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.accessCertification.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<AccessCertificationConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_ACCESS_CERTIFICATION);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.infrastructure.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<InfrastructureConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_INFRASTRUCTURE);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.fullTextSearch.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new ContainerOfSystemConfigurationPanel<FullTextSearchConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_FULL_TEXT_SEARCH);
			}
		});
		
		
		return tabs;
	}
	
	@Override
	public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {
		
	}

	@Override
	public Class<SystemConfigurationType> getCompileTimeClass() {
		return SystemConfigurationType.class;
	}

	@Override
	protected SystemConfigurationType createNewObject() {
		return null;
	}

	@Override
	protected ObjectSummaryPanel<SystemConfigurationType> createSummaryPanel() {
		return new SystemConfigurationSummaryPanel(ID_SUMM_PANEL, SystemConfigurationType.class, Model.of(getObjectModel().getObject().getObject()), this);
	}

	@Override
	protected AbstractObjectMainPanel<SystemConfigurationType> createMainPanel(String id) {
		return new AbstractObjectMainPanel<SystemConfigurationType>(id, getObjectModel(), this) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<ITab> createTabs(PageAdminObjectDetails<SystemConfigurationType> parentPage) {
				return getTabs();
			}
			
			@Override
			protected boolean getOptionsPanelVisibility() {
				return false;
			}
			
			@Override
			protected boolean isPreviewButtonVisible() {
				return false;
			}
			
			@Override
			protected void initLayoutTabs(PageAdminObjectDetails<SystemConfigurationType> parentPage) {
				List<ITab> tabs = createTabs(parentPage);
				TabbedPanel<ITab> tabPanel = new TabbedPanel<ITab>(ID_TAB_PANEL, tabs) {

					private static final long serialVersionUID = 1L;
					
					@Override
					protected void onTabChange(int index) {
						PageParameters params = getPageParameters();
						params.set(SELECTED_TAB_INDEX, index);
						
						parentPage.updateBreadcrumbParameters(SELECTED_TAB_INDEX, index);
					}
				};
				getMainForm().add(tabPanel);
			}
		};
	}

	@Override
	protected Class<? extends Page> getRestartResponsePage() {
		return getMidpointApplication().getHomePage();
	}

	@Override
	public void continueEditing(AjaxRequestTarget target) {
		
	}
	
	@Override
	protected void setSummaryPanelVisibility(ObjectSummaryPanel<SystemConfigurationType> summaryPanel) {
		summaryPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return true;
            }
        });
	}
	
	@Override
	public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly) {
		
		ProgressPanel progressPanel = getProgressPanel();
		progressPanel.hide();
		Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
		super.saveOrPreviewPerformed(target, result, previewOnly, task);
		
		try {
			TimeUnit.SECONDS.sleep(1);
			while(task.isClosed()) {TimeUnit.SECONDS.sleep(1);}
		} catch ( InterruptedException ex) {
			result.recomputeStatus();
	        result.recordFatalError("Couldn't use sleep", ex);

	        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't use sleep", ex);
		}
		result.recomputeStatus();
		target.add(getFeedbackPanel());
		
		if(result.getStatus().equals(OperationResultStatus.SUCCESS)) {
			showResult(result);
			redirectBack();
		}
	}
	
}