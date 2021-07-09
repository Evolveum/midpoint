/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.system;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ContainerOfSystemConfigurationPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.progress.ProgressPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.SystemConfigurationSummaryPanel;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfigurationNew;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;

public abstract class PageAbstractSystemConfiguration extends PageAdminObjectDetails<SystemConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageAbstractSystemConfiguration.class);


    public PageAbstractSystemConfiguration() {
        initialize(null);
    }

    public PageAbstractSystemConfiguration(PageParameters parameters) {
        super(parameters);
        initialize(null);
    }

    public PageAbstractSystemConfiguration(final PrismObject<SystemConfigurationType> configToEdit) {
        super(configToEdit, false);
        initialize(configToEdit, false);
    }

    public PageAbstractSystemConfiguration(final PrismObject<SystemConfigurationType> configToEdit, boolean isNewObject)  {
        super(configToEdit, isNewObject);
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

    @Override
    public Class<SystemConfigurationType> getCompileTimeClass() {
        return SystemConfigurationType.class;
    }

    @Override
    protected SystemConfigurationType createNewObject() {
        return new SystemConfigurationType(getPrismContext());
    }

    @Override
    protected ObjectSummaryPanel<SystemConfigurationType> createSummaryPanel(IModel<SystemConfigurationType> summaryModel) {
        return new SystemConfigurationSummaryPanel(ID_SUMMARY_PANEL, summaryModel, PageAbstractSystemConfiguration.this);
    }

    @Override
    protected void setSummaryPanelVisibility(ObjectSummaryPanel<SystemConfigurationType> summaryPanel) {
        summaryPanel.setVisible(true);
    }

    @Override
    protected AbstractObjectMainPanel<SystemConfigurationType> createMainPanel(String id) {
        return new AbstractObjectMainPanel<>(id, getObjectModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> createTabs(PageAdminObjectDetails<SystemConfigurationType> parentPage) {
                return PageAbstractSystemConfiguration.this.createTabs();
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
        return PageSystemConfigurationNew.class;
    }

    <C extends Containerable> ContainerOfSystemConfigurationPanel<C> createContainerPanel(String panelId, IModel<? extends PrismContainerWrapper<C>> objectModel, ItemName propertyName, QName propertyType) {
        return new ContainerOfSystemConfigurationPanel<>(panelId, createModel(objectModel, propertyName), propertyType);
    }

    <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> createModel(IModel<? extends PrismContainerWrapper<C>> model, ItemName itemName) {
        return PrismContainerWrapperModel.fromContainerWrapper(model, itemName);
    }

    protected abstract List<ITab> createTabs();

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
