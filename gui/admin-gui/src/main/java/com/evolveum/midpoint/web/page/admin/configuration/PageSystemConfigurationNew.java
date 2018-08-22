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
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.configuration.component.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.GlobalPolicyRuleTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.LoggingConfigPanelNew;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.LoggingConfigurationTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.NotificationConfigPanelNew;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.NotificationConfigTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ObjectPolicyConfigurationTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.OneContainerConfigurationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.SystemConfigPanelNew;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.error.PageError;

/**
 * @author lazyman
 * @author skublik
 */
@PageDescriptor(
		urls = {
				@Url(mountUrl = "/admin/config/new", matchUrlForSecurity = "/admin/config/new"),
				//@Url(mountUrl = "/admin/config/system/new"),
		},
		action = {
				@AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
						label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
						description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
						label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
						description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
		})
public class PageSystemConfigurationNew extends PageAdminObjectDetails<SystemConfigurationType> {

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

	private static final String DOT_CLASS = PageSystemConfiguration.class.getName() + ".";
	private static final String TASK_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";
	
	private static final String ID_SUMM_PANEL = "summaryPanel";

	public static final String ROOT_APPENDER_INHERITANCE_CHOICE = "(Inherit root)";

	public PageSystemConfigurationNew() {
		initialize(null);
	}
	
	public PageSystemConfigurationNew(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
		initialize(null);
	}
	
	public PageSystemConfigurationNew(final PrismObject<SystemConfigurationType> userToEdit) {
        initialize(userToEdit);
    }
	
	public PageSystemConfigurationNew(final PrismObject<SystemConfigurationType> unitToEdit, boolean isNewObject)  {
        initialize(unitToEdit, isNewObject);
    }
	
	@Override
	protected void initializeModel(final PrismObject<SystemConfigurationType> objectToEdit, boolean isNewObject, boolean isReadonly) {
		super.initializeModel(WebModelServiceUtils.loadSystemConfigurationAsObjectWrapper(this).getObject(), false, isReadonly);
    }
	
	private List<ITab> getTabs(){
		List<ITab> tabs = new ArrayList<>();
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.system.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new SystemConfigPanelNew(panelId, getObjectModel());
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.objectPolicy.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<ObjectPolicyConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						new ItemPath(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION));
				return new ObjectPolicyConfigurationTabPanel(panelId, model);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.globalPolicyRule.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<GlobalPolicyRuleType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						new ItemPath(SystemConfigurationType.F_GLOBAL_POLICY_RULE));
				return new GlobalPolicyRuleTabPanel(panelId, model);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.globalAccountSynchronization.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<ProjectionPolicyType>(panelId, getObjectModel(), SystemConfigurationType.F_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.cleanupPolicy.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<CleanupPoliciesType>(panelId, getObjectModel(), SystemConfigurationType.F_CLEANUP_POLICY);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.notifications.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<NotificationConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						new ItemPath(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION));
				return new NotificationConfigTabPanel(panelId, model);
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.logging.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				ContainerWrapperFromObjectWrapperModel<LoggingConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectModel(), 
						new ItemPath(SystemConfigurationType.F_LOGGING));
				return new LoggingConfigurationTabPanel(panelId, model);
//				return new LoggingConfigPanelNew(panelId, getObjectModel());
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.profiling.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<ProfilingConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_PROFILING_CONFIGURATION);
			}
		});

		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.adminGui.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<AdminGuiConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.workflow.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<WfConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_WORKFLOW_CONFIGURATION);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.roleManagement.title")) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<RoleManagementConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_ROLE_MANAGEMENT);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.internals.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<InternalsConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_INTERNALS);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.deploymentInformation.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<DeploymentInformationType>(panelId, getObjectModel(), SystemConfigurationType.F_DEPLOYMENT_INFORMATION);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.accessCertification.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<AccessCertificationConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_ACCESS_CERTIFICATION);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.infrastructure.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<InfrastructureConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_INFRASTRUCTURE);
			}
		});
		
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.fullTextSearch.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new OneContainerConfigurationPanel<FullTextSearchConfigurationType>(panelId, getObjectModel(), SystemConfigurationType.F_FULL_TEXT_SEARCH);
			}
		});
		
		
		return tabs;
	}
	
	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();
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
}