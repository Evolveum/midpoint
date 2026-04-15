/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardNavigationBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationItemRefsTable;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.SimulationResultObjectPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@PanelType(name = "rw-simulation-object-result")
@PanelInstance(identifier = "rw-simulation-object-result",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceSimulationResultObjectWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public class ResourceSimulationResultObjectWizardPanel extends AbstractWizardNavigationBasicPanel<ResourceDetailsModel> {

    private static final String PANEL_TYPE = "rw-simulation-object-result";

    private static final String ID_PANEL = "panel";

    IModel<SimulationResultType> resultModel;
    IModel<SimulationResultProcessedObjectType> objectModel;
    IModel<String> markOidModel;

    public ResourceSimulationResultObjectWizardPanel(
            @NotNull String id,
            @NotNull ResourceDetailsModel detailsModel,
            @NotNull IModel<SimulationResultType> simulationResultModel,
            @NotNull IModel<SimulationResultProcessedObjectType> objectModel,
            @Nullable IModel<String> markOid) {
        super(id, detailsModel);
        this.resultModel = simulationResultModel;
        this.objectModel = objectModel;
        this.markOidModel = markOid;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        SimulationResultObjectPanel panel = new SimulationResultObjectPanel(ID_PANEL, resultModel, objectModel) {

            @Override
            protected void navigateToSimulationResultObject(
                    @NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                ResourceSimulationResultObjectWizardPanel.this.navigateToSimulationResultObject(simulationResultOid, markOid, object, target);
            }

            @Override
            protected @Nullable String getMarkOid() {
                return markOidModel != null ? markOidModel.getObject() : null;
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    protected void navigateToSimulationResultObject(
            @NotNull String simulationResultOid,
            @Nullable String markOid,
            @NotNull SimulationResultProcessedObjectType object,
            @NotNull AjaxRequestTarget target) {
        // Override to do something
    }

    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return createStringResource("ResourceSimulationResultObjectWizardPanel.breadcrumb");
    }

    @Override
    public boolean isEnabledInHierarchy() {
        return super.isEnabledInHierarchy();
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("ResourceSimulationResultObjectWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return isSubTextVisible()? createStringResource("ResourceSimulationResultObjectWizardPanel.subText")
                : null;
    }

    protected boolean isSubTextVisible() {
        return false;
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
