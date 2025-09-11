/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationItemRefsTable;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.SimulationResultPanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

@PanelType(name = "rw-simulation-result")
@PanelInstance(identifier = "rw-simulation-result",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceSimulationResultWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public abstract class ResourceSimulationResultWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final String PANEL_TYPE = "rw-simulation-result";
    private static final String ID_PANEL = "panel";

    IModel<SimulationResultType> simulationResultModel;

    public ResourceSimulationResultWizardPanel(
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
        SimulationResultPanel resultPanel = new SimulationResultPanel(ID_PANEL, simulationResultModel) {
            @Override
            protected void navigateToSimulationResultObjects(
                    @NotNull String resultOid,
                    @Nullable ObjectReferenceType ref,
                    @Nullable ObjectProcessingStateType state,
                    @NotNull AjaxRequestTarget target) {
                ResourceSimulationResultWizardPanel.this.navigateToSimulationTasksWizard(resultOid, ref, state, target);
            }
        };
        resultPanel.setOutputMarkupId(true);
        add(resultPanel);
    }

    protected abstract void navigateToSimulationTasksWizard(
            @NotNull String resultOid,
            @Nullable ObjectReferenceType ref,
            @Nullable ObjectProcessingStateType state,
            @NotNull AjaxRequestTarget target);

    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        AjaxIconButton viewProcessedButton = new AjaxIconButton(buttons.newChildId(), () -> "fa-solid fa-magnifying-glass mr-2",
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
        viewProcessedButton.showTitleAsLabel(true);
        viewProcessedButton.add(AttributeAppender.append("class", "btn btn-primary"));
        viewProcessedButton.setOutputMarkupId(true);
        buttons.add(viewProcessedButton);
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return createStringResource("ResourceSimulationResultWizardPanel.breadcrumb");
    }

    @Override
    public boolean isEnabledInHierarchy() {
        return super.isEnabledInHierarchy();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceSimulationResultWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceSimulationResultWizardPanel.subText");
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
