/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

public abstract class PageBaseSystemConfiguration extends PageAssignmentHolderDetails<SystemConfigurationType, AssignmentHolderDetailsModel<SystemConfigurationType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageBaseSystemConfiguration.class);

    public PageBaseSystemConfiguration() {
        super();
    }

    public PageBaseSystemConfiguration(PageParameters parameters) {
        super(parameters);
    }

    public PageBaseSystemConfiguration(final PrismObject<SystemConfigurationType> object) {
        super(object);
    }

    @Override
    public Class<SystemConfigurationType> getType() {
        return SystemConfigurationType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<SystemConfigurationType> model) {
        return new ObjectSummaryPanel(id, SystemConfigurationType.class, model, getSummaryPanelSpecification()) {

            @Override
            protected String getDefaultIconCssClass() {
                return getSummaryIconCssClass();
            }

            @Override
            protected String getIconBoxAdditionalCssClass() {
                return null;
            }

            @Override
            protected String getBoxAdditionalCssClass() {
                return null;
            }

            @Override
            protected IModel<String> getDisplayNameModel() {
                return getPageTitleModel();
            }
        };
    }

    protected String getSummaryIconCssClass() {
        return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON;
    }

    protected IModel<String> getSummaryDisplayNameModel() {
        return getPageTitleModel();
    }

    @Override
    protected String getObjectOidParameter() {
        return SystemObjectsType.SYSTEM_CONFIGURATION.value();
    }

    @Override
    protected AssignmentHolderDetailsModel<SystemConfigurationType> createObjectDetailsModels(PrismObject<SystemConfigurationType> object) {
        return new AssignmentHolderDetailsModel<>(createPrismObjectModel(object), this) {

            @Override
            protected GuiObjectDetailsPageType loadDetailsPageConfiguration(PrismObject<SystemConfigurationType> assignmentHolder) {
                CompiledGuiProfile profile = getModelServiceLocator().getCompiledGuiProfile();
                try {
                    GuiObjectDetailsPageType defaultPageConfig = null;
                    for (Class<? extends Containerable> clazz : getAllDetailsTypes()) {
                        QName type = GuiImplUtil.getContainerableTypeName(clazz);
                        if (defaultPageConfig == null) {
                            defaultPageConfig = profile.findObjectDetailsConfiguration(type);
                        } else {
                            GuiObjectDetailsPageType anotherConfig = profile.findObjectDetailsConfiguration(type);
                            defaultPageConfig = getModelServiceLocator().getAdminGuiConfigurationMergeManager().mergeObjectDetailsPageConfiguration(defaultPageConfig, anotherConfig);
                        }
                    }

                    return applyArchetypePolicy(defaultPageConfig);
                } catch (Exception ex) {
                    LOGGER.error("Couldn't create default gui object details page and apply archetype policy", ex);
                }

                return null;
            }
        };
    }

    public List<Class<? extends Containerable>> getAllDetailsTypes() {
        return Arrays.asList(getDetailsType());
    }

    public Class<? extends Containerable> getDetailsType() {
        return getType();
    }

    //
//    @Override
//    protected PrismObject<SystemConfigurationType> loadPrismObject(PrismObject<SystemConfigurationType> objectToEdit, Task task, OperationResult result) {
//        return WebModelServiceUtils.loadSystemConfigurationAsPrismObject(this, task, result);
//    }
//
//    @Override
//    protected ItemStatus computeWrapperStatus() {
//        return ItemStatus.NOT_CHANGED;
//    }
//
//    @Override
//    public Class<SystemConfigurationType> getCompileTimeClass() {
//        return SystemConfigurationType.class;
//    }
//
//    @Override
//    public SystemConfigurationType createNewObject() {
//        return new SystemConfigurationType(getPrismContext());
//    }
//
//    @Override
//    protected ObjectSummaryPanel<SystemConfigurationType> createSummaryPanel(IModel<SystemConfigurationType> summaryModel) {
//        return new SystemConfigurationSummaryPanel(ID_SUMMARY_PANEL, summaryModel, WebComponentUtil.getSummaryPanelSpecification(SystemConfigurationType.class, getCompiledGuiProfile()));
//    }
//
//    @Override
//    protected void setSummaryPanelVisibility(ObjectSummaryPanel<SystemConfigurationType> summaryPanel) {
//        summaryPanel.setVisible(true);
//    }
//
//    @Override
//    protected AbstractObjectMainPanel<SystemConfigurationType> createMainPanel(String id) {
//        return new AbstractObjectMainPanel<>(id, getObjectModel(), this) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected List<ITab> createTabs(PageAdminObjectDetails<SystemConfigurationType> parentPage) {
//                return PageAbstractSystemConfiguration.this.createTabs();
//            }
//
//            @Override
//            protected boolean getOptionsPanelVisibility() {
//                return false;
//            }
//
//            @Override
//            protected boolean isPreviewButtonVisible() {
//                return false;
//            }
//
//        };
//    }
//
//    @Override
//    protected Class<? extends Page> getRestartResponsePage() {
//        return PageSystemConfigurationNew.class;
//    }
//
//    <C extends Containerable> ContainerOfSystemConfigurationPanel<C> createContainerPanel(String panelId, IModel<? extends PrismContainerWrapper<C>> objectModel, ItemName propertyName, QName propertyType) {
//        return new ContainerOfSystemConfigurationPanel<>(panelId, createModel(objectModel, propertyName), propertyType);
//    }
//
//    <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> createModel(IModel<? extends PrismContainerWrapper<C>> model, ItemName itemName) {
//        return PrismContainerWrapperModel.fromContainerWrapper(model, itemName);
//    }
//
//    protected abstract List<ITab> createTabs();
//
//    @Override
//    public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly) {
//
//        ProgressPanel progressPanel = getProgressPanel();
//        progressPanel.hide();
//        Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
//        super.saveOrPreviewPerformed(target, result, previewOnly, task);
//
//        try {
//            TimeUnit.SECONDS.sleep(1);
//            while(task.isClosed()) {TimeUnit.SECONDS.sleep(1);}
//        } catch ( InterruptedException ex) {
//            result.recomputeStatus();
//            result.recordFatalError(getString("PageSystemConfiguration.message.saveOrPreviewPerformed.fatalError"), ex);
//
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't use sleep", ex);
//        }
//        result.recomputeStatus();
//        target.add(getFeedbackPanel());
//
//        if(result.getStatus().equals(OperationResultStatus.SUCCESS)) {
//            showResult(result);
//            redirectBack();
//        }
//    }
}
