/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationItemRefsTable;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.SimulationResultObjectsPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@PanelType(name = "rw-simulation-objects-result")
@PanelInstance(identifier = "rw-simulation-objects-result",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceSimulationResultObjectsWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public abstract class ResourceSimulationResultObjectsWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final String PANEL_TYPE = "rw-simulation-objects-result";
    private static final String ID_PANEL = "panel";

    IModel<SimulationResultType> resultModel;
    @Nullable ObjectProcessingStateType state;

    public ResourceSimulationResultObjectsWizardPanel(@NotNull String id,
            @NotNull ResourceDetailsModel detailsModel,
            @NotNull IModel<SimulationResultType> simulationResultModel,
            @Nullable ObjectProcessingStateType state) {
        super(id, detailsModel);
        this.resultModel = simulationResultModel;
        this.state = state;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        SimulationResultObjectsPanel panel = new SimulationResultObjectsPanel(ID_PANEL, resultModel) {

            @Override
            protected void navigateToSimulationResultObject(
                    @NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                ResourceSimulationResultObjectsWizardPanel.this.navigateToSimulationResultObject(simulationResultOid, markOid, object, target);
            }

            @Contract(pure = true)
            @Override
            protected @Nullable ObjectProcessingStateType getStateQueryParameter() {
                return state;
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    protected abstract void navigateToSimulationResultObject(
            @NotNull String simulationResultOid,
            @Nullable String markOid,
            @NotNull SimulationResultProcessedObjectType object,
            @NotNull AjaxRequestTarget target);

    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return createStringResource("ResourceSimulationResultObjectsWizardPanel.breadcrumb");
    }

    @Override
    public boolean isEnabledInHierarchy() {
        return super.isEnabledInHierarchy();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceSimulationResultObjectsWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceSimulationResultObjectsWizardPanel.subText");
    }

    protected CorrelationItemRefsTable getTable() {
        return (CorrelationItemRefsTable) get(ID_PANEL);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
