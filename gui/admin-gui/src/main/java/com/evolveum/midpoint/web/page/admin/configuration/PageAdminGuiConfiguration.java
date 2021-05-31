/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.progress.ProgressPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 * @author skublik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system/adminGuiConfig", matchUrlForSecurity = "/admin/config/system/adminGuiConfig"),
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                        label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                        description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageAdminGuiConfiguration extends PageAdminObjectDetails<SystemConfigurationType> {

    private static final long serialVersionUID = 1L;


    private static final String DOT_CLASS = PageAdminGuiConfiguration.class.getName() + ".";
    private static final String OPERATION_LOAD_SYSTEM_CONFIG = DOT_CLASS + "load";

    private static final Trace LOGGER = TraceManager.getTrace(PageAdminGuiConfiguration.class);

    private static final String ID_SUMM_PANEL = "summaryPanel";

    public PageAdminGuiConfiguration() {
        initialize(null);
    }

    public PageAdminGuiConfiguration(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageAdminGuiConfiguration(final PrismObject<SystemConfigurationType> configToEdit) {
        initialize(configToEdit);
    }

    public PageAdminGuiConfiguration(final PrismObject<SystemConfigurationType> configToEdit, boolean isNewObject)  {
        initialize(configToEdit, isNewObject);
    }

    @Override
    protected PrismObject<SystemConfigurationType> loadPrismObject(PrismObject<SystemConfigurationType> objectToEdit, Task task, OperationResult result) {
        return WebModelServiceUtils.loadSystemConfigurationAsPrismObject(this, task, result);
    }

    @Override
    protected ItemStatus computeWrapperStatus() {
        return ItemStatus.NOT_CHANGED;
    }

    private List<ITab> getTabs(){
        List<ITab> tabs = new ArrayList<>();

        PrismContainerWrapperModel<SystemConfigurationType, AdminGuiConfigurationType> adminGuiModel = PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION);

        tabs.add(new AbstractTab(createStringResource("User dashboard links details")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_USER_DASHBOARD_LINK, RichHyperlinkType.COMPLEX_TYPE);
            }
        });

        tabs.add(new AbstractTab(createStringResource("Object collection views")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS, GuiObjectListViewsType.COMPLEX_TYPE);
            }
        });

        tabs.add(new AbstractTab(createStringResource("Object details")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_OBJECT_DETAILS, GuiObjectDetailsSetType.COMPLEX_TYPE);
            }
        });


        tabs.add(new AbstractTab(createStringResource("Additional menu link")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_ADDITIONAL_MENU_LINK, RichHyperlinkType.COMPLEX_TYPE);
            }
        });

        tabs.add(new AbstractTab(createStringResource("Configurable user dashboard")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_CONFIGURABLE_USER_DASHBOARD, ConfigurableUserDashboardType.COMPLEX_TYPE);
            }
        });


        return tabs;
    }

    private <C extends Containerable> ContainerOfSystemConfigurationPanel<C> createContainerPanel(String panelId, IModel<PrismContainerWrapper<AdminGuiConfigurationType>> objectModel, ItemName propertyName, QName propertyType) {
        return new ContainerOfSystemConfigurationPanel<C>(panelId, createModel(objectModel, propertyName), propertyType);
    }

    private <C extends Containerable> PrismContainerWrapperModel<AdminGuiConfigurationType, C> createModel(IModel<PrismContainerWrapper<AdminGuiConfigurationType>> model, ItemName itemName) {
        return PrismContainerWrapperModel.fromContainerWrapper(model, itemName);
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
        if (!isKeepDisplayingResults()) {
            showResult(result);
            redirectBack();
        }
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
    protected ObjectSummaryPanel<SystemConfigurationType> createSummaryPanel(IModel<SystemConfigurationType> summaryModel) {
        return new SystemConfigurationSummaryPanel(ID_SUMM_PANEL, SystemConfigurationType.class, summaryModel, this);
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

//            @Override
//            protected void initLayoutTabs(PageAdminObjectDetails<SystemConfigurationType> parentPage) {
//                List<ITab> tabs = createTabs(parentPage);
//                TabbedPanel<ITab> tabPanel = new TabbedPanel<ITab>(ID_TAB_PANEL, tabs) {
//
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    protected void onTabChange(int index) {
//                        PageParameters params = getPageParameters();
//                        params.set(SELECTED_TAB_INDEX, index);
//
//                        parentPage.updateBreadcrumbParameters(SELECTED_TAB_INDEX, index);
//                    }
//                };
//                getMainForm().add(tabPanel);
//            }
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
            result.recordFatalError(getString("PageSystemConfiguration.message.saveOrPreviewPerformed.fatalError"), ex);

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
