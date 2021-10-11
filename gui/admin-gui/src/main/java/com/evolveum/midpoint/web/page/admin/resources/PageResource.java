/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxTabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.resources.component.TestConnectionResultPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
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
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                        label = "PageAdminResources.auth.resourcesAll.label",
                        description = "PageAdminResources.auth.resourcesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
                        label = "PageResource.auth.resource.label",
                        description = "PageResource.auth.resource.description")
        })
public class PageResource extends PageAdmin {
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

    /**
     * Contains MUTABLE resource object.
     */
    private LoadableModel<PrismObject<ResourceType>> resourceModel;

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
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .noFetch()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .build();
        PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class, resourceOid,
                options, this, task, result);

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
                WebComponentUtil.refreshResourceSchema(resourceModel.getObject(), OPERATION_REFRESH_SCHEMA, target, PageResource.this);
            }
        };
        refreshSchema.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return isVisibleRefresSchemaButton(resourceModel);
            }
        });
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
        configurationEdit.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return canEdit(resourceModel);
            }
        });
        add(configurationEdit);
        AjaxButton wizardShow = new AjaxButton(BUTTON_WIZARD_SHOW_ID,
                createStringResource("pageResource.button.wizardShow")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                startWizard(false, true);
            }
        };
        wizardShow.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return canEdit(resourceModel);
            }
        });
        add(wizardShow);
        AjaxButton wizardEdit = new AjaxButton(BUTTON_WIZARD_EDIT_ID,
                createStringResource("pageResource.button.wizardEdit")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                startWizard(false, false);
            }
        };
        wizardEdit.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return canEdit(resourceModel);
            }
        });
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

    private boolean isVisibleRefresSchemaButton(LoadableModel<PrismObject<ResourceType>> resourceModel) {
        ResourceType resource = resourceModel.getObject().asObjectable();
        if (!resource.getAdditionalConnector().isEmpty()) {
            if (resource.getCapabilities() == null) {
                return false;
            }
            if (resource.getCapabilities().getConfigured() != null) {
                SchemaCapabilityType configuredCapability = CapabilityUtil.getCapability(resource.getCapabilities().getConfigured().getAny(), SchemaCapabilityType.class);
                if (configuredCapability == null) {
                    return false;
                }
                return configuredCapability.isEnabled();
            }
            return false;
        }
        return true;
    }

    private boolean canEdit(LoadableModel<PrismObject<ResourceType>> resourceModel) {
        ResourceType resource = resourceModel.getObject().asObjectable();
        if (!resource.getAdditionalConnector().isEmpty()) {
            return false;
        }
        return true;
    }

    private void startWizard(boolean configOnly, boolean readOnly) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, resourceModel.getObject().getOid());        // compatibility with PageAdminResources
        parameters.add(PageResourceWizard.PARAM_CONFIG_ONLY, configOnly);
        parameters.add(PageResourceWizard.PARAM_READ_ONLY, readOnly);
        navigateToNext(new PageResourceWizard(parameters));
    }

    private ResourceSummaryPanel createResourceSummaryPanel(){
         ResourceSummaryPanel resourceSummaryPanel = new ResourceSummaryPanel(PANEL_RESOURCE_SUMMARY,
                 Model.of(resourceModel.getObject().asObjectable()), this);
         resourceSummaryPanel.setOutputMarkupId(true);
            return resourceSummaryPanel;
    }

    private AjaxTabbedPanel<ITab> createTabsPanel(){
        List<ITab> tabs = new ArrayList<>();

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
                return new ResourceTasksPanel(panelId, resourceModel, PageResource.this);
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


                };
        testConnectionPanel.setOutputMarkupId(true);

        getMainPopup().setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                refreshStatus(target);
                return true;
            }
        });

        showMainPopup(testConnectionPanel, target);

    }

    private void refreshStatus(AjaxRequestTarget target) {
        target.add(addOrReplace(createResourceSummaryPanel()));
        target.add(addOrReplace(createTabsPanel()));
    }
}
