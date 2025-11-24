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
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.correaltion.SimulationCorrelationPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@PanelType(name = "rw-correlation-simulation-result")
@PanelInstance(identifier = "rw-correlation-simulation-result",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceSimulationResultWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public abstract class ResourceCorrelationSimulationResultWizardPanel extends AbstractWizardNavigationBasicPanel<ResourceDetailsModel> {

    private static final String PANEL_TYPE = "rw-simulation-result";
    private static final String ID_PANEL = "panel";

    IModel<SimulationResultType> simulationResultModel;

    public ResourceCorrelationSimulationResultWizardPanel(
            @NotNull String id,
            @NotNull ResourceDetailsModel detailsModel,
            @NotNull IModel<SimulationResultType> simulationResultModel) {
        super(id, detailsModel);
        this.simulationResultModel = simulationResultModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        SimulationCorrelationPanel resultPanel = new SimulationCorrelationPanel(ID_PANEL, simulationResultModel) {
            @Override
            protected void navigateToSimulationResultObject(
                    @NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                ResourceCorrelationSimulationResultWizardPanel.this.navigateToSimulationResultObject(
                        simulationResultOid, markOid, object, target);
            }
        };
        resultPanel.setOutputMarkupId(true);
        add(resultPanel);
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        AjaxIconButton button = new AjaxIconButton(buttons.newChildId(), () -> "fa-solid fa-magnifying-glass mr-2",
                () -> getString("PageSimulationResult.viewProcessedObjects")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                navigateToSimulationTasksWizard(
                        simulationResultModel.getObject().getOid(),
                        null,
                        null,
                        ajaxRequestTarget);
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeAppender.append("class", "btn btn-primary ml-auto"));
        button.setOutputMarkupId(true);
        buttons.add(button);
    }

    protected abstract void navigateToSimulationTasksWizard(
            @NotNull String resultOid,
            @Nullable ObjectReferenceType ref,
            @Nullable ObjectProcessingStateType state,
            @NotNull AjaxRequestTarget target);

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
        return createStringResource("ResourceCorrelationSimulationResultWizardPanel.breadcrumb");
    }

    @Override
    public boolean isEnabledInHierarchy() {
        return super.isEnabledInHierarchy();
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("ResourceCorrelationSimulationResultWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return isSubTextVisible() ? createStringResource("ResourceCorrelationSimulationResultWizardPanel.subText")
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
