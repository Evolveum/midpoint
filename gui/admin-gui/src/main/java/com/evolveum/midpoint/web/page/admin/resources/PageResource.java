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

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.util.ResourceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxTabbedPanel;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.resources.component.TestConnectionResultPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author katkav
 */
@PageDescriptor(
		urls = {
				@Url(mountUrl = "/admin/resource", matchUrlForSecurity = "/admin/resource")
		},
		action = {
				@AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL,
						label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL,
						description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
						label = "PageResource.auth.resource.label",
						description = "PageResource.auth.resource.description")
		})
public class PageResource extends PageAdminResources {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageResource.class);

	private static final String DOT_CLASS = PageResource.class.getName() + ".";

	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
	private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";

	private static final String ID_TAB_PANEL = "tabPanel";

	private static final String PANEL_RESOURCE_SUMMARY = "summary";

	private static final String BUTTON_TEST_CONNECTION_ID = "testConnection";
	private static final String BUTTON_REFRESH_SCHEMA_ID = "refreshSchema";
	private static final String BUTTON_EDIT_XML_ID = "editXml";
	private static final String BUTTON_CONFIGURATION_EDIT_ID = "configurationEdit";
	private static final String BUTTON_WIZARD_EDIT_ID = "wizardEdit";
	private static final String BUTTON_WIZARD_SHOW_ID = "wizardShow";
	private static final String ID_BUTTON_BACK = "back";

	public static final String TABLE_TEST_CONNECTION_RESULT_ID = "testConnectionResults";

	public static final String PARAMETER_SELECTED_TAB = "tab";

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

	@Override
	protected void onConfigure() {
		super.onConfigure();

		AjaxTabbedPanel tabbedPanel = (AjaxTabbedPanel) get(ID_TAB_PANEL);
		WebComponentUtil.setSelectedTabFromPageParameters(tabbedPanel, getPageParameters(), PARAMETER_SELECTED_TAB);
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
		resolveConnectorOption.add(SelectorOptions.create(GetOperationOptions.createNoFetch()));
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
				navigateToNext(PageDebugView.class, parameters);
			}
		};
		add(editXml);
		AjaxButton configurationEdit = new AjaxButton(BUTTON_CONFIGURATION_EDIT_ID,
				createStringResource("pageResource.button.configurationEdit")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				startWizard(true, false);
			}
		};
		add(configurationEdit);
		AjaxButton wizardShow = new AjaxButton(BUTTON_WIZARD_SHOW_ID,
				createStringResource("pageResource.button.wizardShow")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				startWizard(false, true);
			}
		};
		add(wizardShow);
		AjaxButton wizardEdit = new AjaxButton(BUTTON_WIZARD_EDIT_ID,
				createStringResource("pageResource.button.wizardEdit")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				startWizard(false, false);
			}
		};
		add(wizardEdit);

		AjaxButton back = new AjaxButton(ID_BUTTON_BACK, createStringResource("pageResource.button.back")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				redirectBack();
			}
		};
		add(back);

	}

	private void startWizard(boolean configOnly, boolean readOnly) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, resourceModel.getObject().getOid());		// compatibility with PageAdminResources
		parameters.add(PageResourceWizard.PARAM_CONFIG_ONLY, configOnly);
		parameters.add(PageResourceWizard.PARAM_READ_ONLY, readOnly);
		navigateToNext(new PageResourceWizard(parameters));
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
		
		tabs.add(new PanelTab(createStringResource("PageResource.tab.connector")) {
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new ResourceConnectorPanel(panelId, null, resourceModel, PageResource.this);
			}
		});

		AjaxTabbedPanel<ITab> resourceTabs = new AjaxTabbedPanel<ITab>(ID_TAB_PANEL, tabs) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTabChange(int index) {
				updateBreadcrumbParameters(PARAMETER_SELECTED_TAB, index);
			}
		};
		resourceTabs.setOutputMarkupId(true);
		return resourceTabs;
	}
	
	

	private void refreshSchemaPerformed(AjaxRequestTarget target) {

		Task task = createSimpleTask(OPERATION_REFRESH_SCHEMA);
		OperationResult parentResult = new OperationResult(OPERATION_REFRESH_SCHEMA);

		try {
			ResourceUtils.deleteSchema(resourceModel.getObject(), getModelService(), getPrismContext(), task, parentResult);
			getModelService().testResource(resourceModel.getObject().getOid(), task);					// try to load fresh scehma
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Error refreshing resource schema", e);
			parentResult.recordFatalError("Error refreshing resource schema", e);
		}

		parentResult.computeStatus();
		showResult(parentResult, "pageResource.refreshSchema.failed");
		target.add(getFeedbackPanel());
	}

	private void testConnectionPerformed(AjaxRequestTarget target) {
		final PrismObject<ResourceType> dto = resourceModel.getObject();
		if (dto == null || StringUtils.isEmpty(dto.getOid())) {
			error(getString("pageResource.message.oidNotDefined"));
			target.add(getFeedbackPanel());
			return;
		}

        final TestConnectionResultPanel testConnectionPanel =
                new TestConnectionResultPanel(getMainPopupBodyId(),
						dto.getOid(), getPage()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void okPerformed(AjaxRequestTarget target) {
                        refreshStatus(target);
                    }

//                    @Override
//                    protected void initOnFocusBehavior() {
//                        setOnFocusBehavior(new AjaxEventBehavior("focus") {
//
//                        	private static final long serialVersionUID = 1L;
//
//                            @Override
//                            protected void onEvent(AjaxRequestTarget target) {
//                                removeOnFocusBehavior(getOkButton());
//                                OperationResult result = new OperationResult(OPERATION_TEST_CONNECTION);
//                                List<OpResult>  resultsDto = new ArrayList<>();
//                                try {
//                                    Task task = createSimpleTask(OPERATION_TEST_CONNECTION);
//                                    result = getModelService().testResource(dto.getOid(), task);
//                                    resultsDto = WebComponentUtil.getTestConnectionResults(result,(PageBase) getPage());
//
//                                    getModelService().getObject(ResourceType.class, dto.getOid(), null, task, result);
//                                } catch (ObjectNotFoundException | SchemaException | SecurityViolationException
//                                        | CommunicationException | ConfigurationException e) {
//                                    result.recordFatalError("Failed to test resource connection", e);
//                                }
//
//                                if (result.isSuccess()) {
//                                    result.recomputeStatus();
//                                }
//                                setModelObject(resultsDto);
//                                initResultsPanel((RepeatingView) getResultsComponent(), getPage());
//                                setWaitForResults(false);
//                                target.add(getContentPanel());
//                            }
//                        });
//                    }
                };
        testConnectionPanel.setOutputMarkupId(true);

        getMainPopup().setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return false;
            }
        });

        showMainPopup(testConnectionPanel, target);
//        if (!testConnectionPanel.isFocusSet()) {
//            testConnectionPanel.setFocusSet(true);
//            testConnectionPanel.setFocusOnComponent(testConnectionPanel.getOkButton(), target);
//        }

	}
	
	private void refreshStatus(AjaxRequestTarget target) {
		target.add(addOrReplace(createResourceSummaryPanel()));
		target.add(addOrReplace(createTabsPanel()));
	}
}
