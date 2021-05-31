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

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
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
public class PageSystemConfigurationNew extends PageAdminObjectDetails<SystemConfigurationType> {

    private static final long serialVersionUID = 1L;

    public static final String SELECTED_TAB_INDEX = "tab";
    public static final String SELECTED_SERVER_INDEX = "mailServerIndex";
    public static final String SERVER_LIST_SIZE = "mailServerListSize";

    private static final String DOT_CLASS = PageSystemConfigurationNew.class.getName() + ".";
    private static final String OPERATION_LOAD_SYSTEM_CONFIG = DOT_CLASS + "load";

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

    private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfigurationNew.class);

    private static final String ID_SUMM_PANEL = "summaryPanel";

    public static final String ROOT_APPENDER_INHERITANCE_CHOICE = "(Inherit root)";

    public PageSystemConfigurationNew() {
        initialize(null);
    }

    public PageSystemConfigurationNew(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageSystemConfigurationNew(final PrismObject<SystemConfigurationType> configToEdit) {
        initialize(configToEdit);
    }

    public PageSystemConfigurationNew(final PrismObject<SystemConfigurationType> configToEdit, boolean isNewObject)  {
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

    //    @Override
//    protected void initializeModel(final PrismObject<SystemConfigurationType> configToEdit, boolean isNewObject, boolean isReadonly) {
//        Task task = createSimpleTask(OPERATION_LOAD_SYSTEM_CONFIG);
//        OperationResult result = new OperationResult(OPERATION_LOAD_SYSTEM_CONFIG);
////        super.initializeModel(WebModelServiceUtils.loadSystemConfigurationAsPrismObject(this, task, result), false, isReadonly);
//    }


    private <C extends Containerable> ContainerOfSystemConfigurationPanel<C> createContainerPanel(String panelId, IModel<PrismObjectWrapper<SystemConfigurationType>> objectModel, ItemName propertyName, QName propertyType) {
        return new ContainerOfSystemConfigurationPanel<C>(panelId, createModel(objectModel, propertyName), propertyType);
    }

    private <C extends Containerable> PrismContainerWrapperModel<SystemConfigurationType, C> createModel(IModel<PrismObjectWrapper<SystemConfigurationType>> model, ItemName itemName) {
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
                List<ITab> tabs = new ArrayList<>();
                tabs.add(new PanelTab(createStringResource("")) {
                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        IModel<List<CompositedIconButtonDto>> model = new LoadableModel<>() {
                            @Override
                            protected List<CompositedIconButtonDto> load() {
                                List<CompositedIconButtonDto> buttons = new ArrayList<>();
                                buttons.add(createCompositedButton("Basic", "fa fa-wrench", ItemPath.EMPTY_PATH));
                                buttons.add(createCompositedButton("Object policies", "fa  fa-umbrella", SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION));
                                buttons.add(createCompositedButton("Global policy rule", "fa fa-eye", SystemConfigurationType.F_GLOBAL_POLICY_RULE));
                                buttons.add(createCompositedButton("Global projection policy", "fa fa-globe", SystemConfigurationType.F_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS));
                                buttons.add(createCompositedButton("Cleanup policy", "fa  fa-eraser", SystemConfigurationType.F_CLEANUP_POLICY));
                                buttons.add(createCompositedButton("Notifications", "fa fa-envelope", SystemConfigurationType.F_NOTIFICATION_CONFIGURATION));
                                buttons.add(createCompositedButton("Logging", "fa fa-file-text", SystemConfigurationType.F_LOGGING));
                                buttons.add(createCompositedButton("Admin GUI configuration", "fa fa-camera", SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION));
                                return buttons;
                            }
                        };
                        MultiCompositedButtonPanel panel = new MultiCompositedButtonPanel(panelId, model) {
                            @Override
                            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc, CompiledObjectCollectionView collectionViews, ItemPath itemPath) {
                                Class<? extends WebPage> page = getNextPage(itemPath);
                                if (page != null) {
                                    navigateToNext(page);
                                }
                            }
                        };
                        panel.add(AttributeModifier.append("class", " row"));
                        return panel;
                    }
                });
                return tabs;
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

    private Class<? extends WebPage> getNextPage(ItemPath path) {
        if (path.equivalent(SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION)) {
            return PageAdminGuiConfiguration.class;
        }
        return null;
    }

    private CompositedIconButtonDto createCompositedButton(String type, String icon, ItemPath path) {
        CompositedIconButtonDto button = new CompositedIconButtonDto();
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setTitle(type);
        builder.setBasicIcon(icon, IconCssStyle.CENTER_STYLE);
        button.setCompositedIcon(builder.build());
        DisplayType displayType = new DisplayType();
        displayType.setLabel(new PolyStringType(type));
        button.setAdditionalButtonDisplayType(displayType);
        button.setPath(path);
        return button;
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
