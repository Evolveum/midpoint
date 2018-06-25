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

package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.configuration.component.*;
import com.evolveum.midpoint.web.page.admin.configuration.dto.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.component.prism.PrismValuePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
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
public class PageSystemConfigurationNew extends PageAdminConfiguration {

	private static final long serialVersionUID = 1L;
	
	public static final String SELECTED_TAB_INDEX = "tab";
	public static final String SELECTED_SERVER_INDEX = "mailServerIndex";
	public static final String SERVER_LIST_SIZE = "mailServerListSize";

	public static final int CONFIGURATION_TAB_BASIC = 0;
	public static final int CONFIGURATION_TAB_NOTIFICATION = 1;
	public static final int CONFIGURATION_TAB_LOGGING = 2;
	public static final int CONFIGURATION_TAB_PROFILING = 3;
	public static final int CONFIGURATION_TAB_ADMIN_GUI = 4;

	private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfiguration.class);

	private static final String DOT_CLASS = PageSystemConfiguration.class.getName() + ".";
	private static final String TASK_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";
	private static final String TASK_UPDATE_SYSTEM_CONFIG = DOT_CLASS + "updateSystemConfiguration";
	
	private static final String ID_SYSTEM_CONFIG = "systemConfigurationNew";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TAB_PANEL = "tabPanel";
	private static final String ID_CANCEL = "cancel";
	private static final String ID_SAVE = "save";

	public static final String ROOT_APPENDER_INHERITANCE_CHOICE = "(Inherit root)";

	private LoggingConfigPanel loggingConfigPanel;
	private ProfilingConfigPanel profilingConfigPanel;
	private SystemConfigPanelNew SystemConfigPanelNew;
	private AdminGuiConfigPanel adminGuiConfigPanel;
	private NotificationConfigPanel notificationConfigPanel;

	private LoadableModel<ObjectWrapper<SystemConfigurationType>> modelWrapp;

	private boolean initialized;

	public PageSystemConfigurationNew() {
		this(null);
	}

	public PageSystemConfigurationNew(PageParameters parameters) {
		super(parameters);
		
		modelWrapp = new LoadableModel<ObjectWrapper<SystemConfigurationType>>(false) {

			private static final long serialVersionUID = 1L;
			
			@Override
			protected ObjectWrapper<SystemConfigurationType> load() {
				return loadSystemConfigurationAsWrapperObject();
			}
			
		};

		initLayout();
	}

	private ObjectWrapper<SystemConfigurationType> loadSystemConfigurationAsWrapperObject() {
		Task task = createSimpleTask(TASK_GET_SYSTEM_CONFIG);
		OperationResult result = new OperationResult(TASK_GET_SYSTEM_CONFIG);

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
			GetOperationOptions.createResolve(), SystemConfigurationType.F_DEFAULT_USER_TEMPLATE,
			SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY);

		ObjectWrapper<SystemConfigurationType> wrapper = null;
		try {
			PrismObject<SystemConfigurationType> systemConfig = WebModelServiceUtils.loadObject(
				SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), options,
				this, task, result);
		
			ObjectWrapperFactory owf = new ObjectWrapperFactory(this);
		
			wrapper = owf.createObjectWrapper("adminPage.systemConfiguration", null, systemConfig, ContainerStatus.MODIFYING, task);
		
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load system configuration", ex);
			result.recordFatalError("Couldn't load system configuration.", ex);
		}

		// what do you do with null? many components depends on this not to be
		// null :)
		if (!WebComponentUtil.isSuccessOrHandledError(result) || wrapper == null) {
			showResult(result, false);
			throw getRestartResponseException(PageError.class);
		}

		return wrapper;
	}
	
	private void initLayout() {
		
		Form mainForm = new Form<>(ID_MAIN_FORM, true);
		add(mainForm);
		
		List<ITab> tabs = new ArrayList<>();
		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.system.title")) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				SystemConfigPanelNew = new SystemConfigPanelNew(panelId, modelWrapp, mainForm, getPageBase());
				return SystemConfigPanelNew;
			}
		});
		
		TabbedPanel<ITab> tabPanel = new TabbedPanel<ITab>(ID_TAB_PANEL, tabs) {
			
			private static final long serialVersionUID = 1L;
						
			@Override
			protected void onTabChange(int index) {
				PageParameters params = getPageParameters();
				params.set(SELECTED_TAB_INDEX, index);
			}
		};
		tabPanel.setOutputMarkupId(true);
		mainForm.add(tabPanel);
		
		initButtons(mainForm);
		
	}
	
	
	private PageBase getPageBase() {
		return this;
	}
	
//		Form mainForm = new Form(ID_MAIN_FORM, true);
//		add(mainForm);
//
//		List<ITab> tabs = new ArrayList<>();
//		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.system.title")) {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public WebMarkupContainer getPanel(String panelId) {
//				systemConfigPanel = new SystemConfigPanel(panelId, modelWrapp);
//				return systemConfigPanel;
//			}
//		});
//
//		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.notifications.title")) {
//
//			private static final long serialVersionUID = 1L;
//			
//			@Override
//			public WebMarkupContainer getPanel(String panelId) {
//				notificationConfigPanel = new NotificationConfigPanel(panelId,
//                    new PropertyModel<>(model, "notificationConfig"), getPageParameters());
//				return notificationConfigPanel;
//			}
//		});
//
//		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.logging.title")) {
//
//			private static final long serialVersionUID = 1L;
//			
//			@Override
//			public WebMarkupContainer getPanel(String panelId) {
//				loggingConfigPanel = new LoggingConfigPanel(panelId,
//                    new PropertyModel<>(model, "loggingConfig"));
//				return loggingConfigPanel;
//			}
//		});
//
//		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.profiling.title")) {
//
//			private static final long serialVersionUID = 1L;
//			
//			@Override
//			public WebMarkupContainer getPanel(String panelId) {
//				profilingConfigPanel = new ProfilingConfigPanel(panelId,
//                    new PropertyModel<>(model, "profilingDto"), PageSystemConfiguration.this);
//				return profilingConfigPanel;
//			}
//		});
//
//		tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.adminGui.title")) {
//
//			private static final long serialVersionUID = 1L;
//			
//			@Override
//			public WebMarkupContainer getPanel(String panelId) {
//                adminGuiConfigPanel = new AdminGuiConfigPanel(panelId, model);
//				return adminGuiConfigPanel;
//			}
//		});
//
//		TabbedPanel<ITab> tabPanel = new TabbedPanel<ITab>(ID_TAB_PANEL, tabs) {
//
//			private static final long serialVersionUID = 1L;
//			
//			@Override
//			protected void onTabChange(int index) {
//				PageParameters params = getPageParameters();
//				params.set(SELECTED_TAB_INDEX, index);
//			}
//		};
//		tabPanel.setOutputMarkupId(true);
//		mainForm.add(tabPanel);
//
//		initButtons(mainForm);
//	}

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

			private static final long serialVersionUID = 1L;
			
			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(save);

		AjaxButton cancel = new AjaxButton(ID_CANCEL, createStringResource("PageBase.button.cancel")) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		mainForm.add(cancel);
	}


	private TabbedPanel<ITab> getTabPanel() {
		return (TabbedPanel) get(createComponentPath(ID_MAIN_FORM, ID_TAB_PANEL));
	}


	private void savePerformed(AjaxRequestTarget target) {
		OperationResult result = new OperationResult(TASK_UPDATE_SYSTEM_CONFIG);
		Task task = createSimpleTask(TASK_UPDATE_SYSTEM_CONFIG);
		try {

			ObjectDelta<SystemConfigurationType> delta = ObjectDelta.summarize(modelWrapp.getObject().getOldDelta(), modelWrapp.getObject().getObjectDelta());
			
			delta.normalize();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("System configuration delta:\n{}", delta.debugDump());
			}
			if (!delta.isEmpty()) {
				getPrismContext().adopt(delta);
				getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task, result);
			}

			result.computeStatusIfUnknown();
		} catch (Exception e) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't save system configuration.", e);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save system configuration.", e);
		}

		showResult(result);
		target.add(getFeedbackPanel());
		resetPerformed(target);
	}

	private void resetPerformed(AjaxRequestTarget target) {
		int index = getTabPanel().getSelectedTab();

		PageParameters params = new PageParameters();
		StringValue mailServerIndex = target.getPageParameters().get(SELECTED_SERVER_INDEX);
		StringValue mailServerSize = target.getPageParameters().get(SERVER_LIST_SIZE);
		if (mailServerIndex != null && mailServerIndex.toString() != null) {
			params.add(SELECTED_SERVER_INDEX, mailServerIndex);
		}
		if (mailServerSize != null && mailServerSize.toString() != null) {
			params.add(SERVER_LIST_SIZE, mailServerSize);
		}
		params.add(SELECTED_TAB_INDEX, index);
		PageSystemConfigurationNew page = new PageSystemConfigurationNew(params);
		setResponsePage(page);
	}

	private void cancelPerformed(AjaxRequestTarget target) {
		resetPerformed(target);
	}
}