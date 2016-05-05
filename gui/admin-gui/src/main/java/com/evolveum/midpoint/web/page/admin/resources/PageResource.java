/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxTabbedPanel;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.resources.component.TestConnectionResultPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author katkav
 */
@PageDescriptor(url = "/admin/resource", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL, label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL, description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL, label = "PageResource.auth.resource.label", description = "PageResource.auth.resource.description") })
public class PageResource extends PageAdminResources {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageResource.class);

	private static final String DOT_CLASS = PageResource.class.getName() + ".";

	private static final String OPERATION_TEST_CONNECTION = DOT_CLASS + "testConnection";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
	private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";

	private static final String ID_TAB_PANEL = "tabPanel";

	private static final String PANEL_RESOURCE_SUMMARY = "summary";

	private static final String BUTTON_TEST_CONNECTION_ID = "testConnection";
	private static final String BUTTON_REFRESH_SCHEMA_ID = "refreshSchema";
	private static final String BUTTON_EDIT_XML_ID = "editXml";
	private static final String BUTTON_WIZARD_ID = "wizard";
	private static final String ID_BUTTON_BACK = "back";

	public static final String TABLE_TEST_CONNECTION_RESULT_ID = "testConņectionResults";

	LoadableModel<PrismObject<ResourceType>> resourceModel;

	private String resourceOid;

	public PageResource(PageParameters parameters) {
		resourceOid = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
		initialize();
	}
	
	public PageResource() {
		initialize();
	}

	private void initialize() {
		resourceModel = new LoadableModel<PrismObject<ResourceType>>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected PrismObject<ResourceType> load() {
				return loadResource();
			}
		};
		initLayout();
	}

	protected String getResourceOid() {
		return resourceOid;
	}

	private PrismObject<ResourceType> loadResource() {
		String resourceOid = getResourceOid();
		LOGGER.trace("Loading resource with oid: {}", resourceOid);

		Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
		Collection<SelectorOptions<GetOperationOptions>> resolveConnectorOption = SelectorOptions
				.createCollection(ResourceType.F_CONNECTOR, GetOperationOptions.createResolve());
		PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class, resourceOid,
				resolveConnectorOption, this, task, result);

		result.recomputeStatus();
		showResult(result, "pageAdminResources.message.cantLoadResource", false);

		return resource;
	}

	private void initLayout() {
		if (resourceModel == null || resourceModel.getObject() == null) {
			return;
		}

		addOrReplace(createResourceSummaryPanel());

		addOrReplace(createTabsPanel());
		
		

		AjaxButton test = new AjaxButton(BUTTON_TEST_CONNECTION_ID,
				createStringResource("pageResource.button.test")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				testConnectionPerformed(target);
			}
		};
		add(test);

		AjaxButton refreshSchema = new AjaxButton(BUTTON_REFRESH_SCHEMA_ID,
				createStringResource("pageResource.button.refreshSchema")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				refreshSchemaPerformed(target);
			}
		};
		add(refreshSchema);
		AjaxButton editXml = new AjaxButton(BUTTON_EDIT_XML_ID,
				createStringResource("pageResource.button.editXml")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				PageParameters parameters = new PageParameters();
				parameters.add(PageDebugView.PARAM_OBJECT_ID, resourceModel.getObject().getOid());
				parameters.add(PageDebugView.PARAM_OBJECT_TYPE, "ResourceType");
				setResponsePage(PageDebugView.class, parameters);
			}
		};
		add(editXml);
		AjaxButton wizard = new AjaxButton(BUTTON_WIZARD_ID,
				createStringResource("pageResource.button.wizard")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				PageParameters parameters = new PageParameters();
				parameters.add(OnePageParameterEncoder.PARAMETER, resourceModel.getObject().getOid());
				setResponsePage(new PageResourceWizard(parameters));
			}
		};
		add(wizard);

		AjaxButton back = new AjaxButton(ID_BUTTON_BACK, createStringResource("pageResource.button.back")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				redirectBack();
			}
		};
		add(back);

	}
	
	private ResourceSummaryPanel createResourceSummaryPanel(){
		 ResourceSummaryPanel resourceSummaryPanel = new ResourceSummaryPanel(PANEL_RESOURCE_SUMMARY,
					resourceModel);
		 resourceSummaryPanel.setOutputMarkupId(true);
			return resourceSummaryPanel;
	}
	
	private AjaxTabbedPanel<ITab> createTabsPanel(){
		List<ITab> tabs = new ArrayList<ITab>();

		tabs.add(new PanelTab(createStringResource("PageResource.tab.details")) {
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new ResourceDetailsTabPanel(panelId, resourceModel, PageResource.this);
			}
		});

		tabs.add(new PanelTab(createStringResource("PageResource.tab.content.tasks")) {
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new ResourceTasksPanel(panelId, true, resourceModel, PageResource.this);
			}
		});

		tabs.add(new PanelTab(createStringResource("PageResource.tab.content.account")) {
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new ResourceContentTabPanel(panelId, ShadowKindType.ACCOUNT, resourceModel,
						PageResource.this);
			}
		});

		tabs.add(new PanelTab(createStringResource("PageResource.tab.content.entitlement")) {
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new ResourceContentTabPanel(panelId, ShadowKindType.ENTITLEMENT, resourceModel,
						PageResource.this);
			}
		});

		tabs.add(new PanelTab(createStringResource("PageResource.tab.content.generic")) {
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new ResourceContentTabPanel(panelId, ShadowKindType.GENERIC, resourceModel,
						PageResource.this);
			}
		});

		tabs.add(new PanelTab(createStringResource("PageResource.tab.content.others")) {
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new ResourceContentTabPanel(panelId, null, resourceModel, PageResource.this);
			}
		});

		AjaxTabbedPanel<ITab> resourceTabs = new AjaxTabbedPanel<>(ID_TAB_PANEL, tabs);
		resourceTabs.setOutputMarkupId(true);
		return resourceTabs;
	}

	private void refreshSchemaPerformed(AjaxRequestTarget target) {

		Task task = createSimpleTask(OPERATION_REFRESH_SCHEMA);
		OperationResult parentResult = new OperationResult(OPERATION_REFRESH_SCHEMA);

		try {

			PrismContainer<XmlSchemaType> resourceSchemaContainer = resourceModel.getObject()
					.findContainer(ResourceType.F_SCHEMA);

			if (resourceSchemaContainer != null && resourceSchemaContainer.getValue() != null) {
				PrismProperty<SchemaDefinitionType> resourceSchemaDefinitionProp = resourceSchemaContainer
						.findProperty(XmlSchemaType.F_DEFINITION);

				if (resourceSchemaDefinitionProp != null && !resourceSchemaDefinitionProp.isEmpty()) {

					PrismPropertyValue<SchemaDefinitionType> resourceSchema = resourceSchemaDefinitionProp
							.getValue().clone();
					ObjectDelta<ResourceType> deleteSchemaDefinitionDelta = ObjectDelta
							.createModificationDeleteProperty(ResourceType.class,
									resourceModel.getObject().getOid(),
									new ItemPath(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION),
									getPrismContext(), resourceSchema.getValue());
					// delete schema
					getModelService().executeChanges(
							(Collection) MiscUtil.createCollection(deleteSchemaDefinitionDelta), null, task,
							parentResult);

				}

			}

			// try to load fresh scehma
			getModelService().testResource(resourceModel.getObject().getOid(), task);

		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Error deleting resource schema: " + e.getMessage(), e);
			parentResult.recordFatalError("Error deleting resource schema", e);
		}

		parentResult.computeStatus();
		showResult(parentResult, "pageResource.refreshSchema.failed");
		target.add(getFeedbackPanel());
	}

	private void testConnectionPerformed(AjaxRequestTarget target) {
		PrismObject<ResourceType> dto = resourceModel.getObject();
		if (dto == null || StringUtils.isEmpty(dto.getOid())) {
			error(getString("pageResource.message.oidNotDefined"));
			target.add(getFeedbackPanel());
			return;
		}

		OperationResult result = new OperationResult(OPERATION_TEST_CONNECTION);
		List<OpResult>  resultsDto = new ArrayList<>();
		try {
			Task task = createSimpleTask(OPERATION_TEST_CONNECTION);

			result = getModelService().testResource(dto.getOid(), task);

			resultsDto = WebComponentUtil.getTestConnectionResults(result, this);

			getModelService().getObject(ResourceType.class, dto.getOid(), null, task, result);
		} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
				| CommunicationException | ConfigurationException e) {
			result.recordFatalError("Failed to test resource connection", e);
		}

		// // a bit of hack: result of TestConnection contains a result of
		// getObject as a subresult
		// // so in case of TestConnection succeeding we recompute the result to
		// show any (potential) getObject problems
		if (result.isSuccess()) {
			result.recomputeStatus();
		}

		TestConnectionResultPanel testConnectionPanel = new TestConnectionResultPanel(getMainPopupBodyId(), new ListModel<OpResult>(resultsDto)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void okPerformed(AjaxRequestTarget target) {
				refreshStatus(target);
			}
		};
		testConnectionPanel.setOutputMarkupId(true);
		getMainPopup().setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
			private static final long serialVersionUID = 1L;

			@Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return false;
            }
        });

		showMainPopup(testConnectionPanel, new Model<String>("Test connection result"), target, 800, 500);

	}
	
	private void refreshStatus(AjaxRequestTarget target) {
		target.add(addOrReplace(createResourceSummaryPanel()));
		target.add(addOrReplace(createTabsPanel()));
	}
}
