/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.loadSimulationResult;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.loadSimulationResultProcessedObjectModel;

import java.io.Serial;

import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.component.dialog.AdditionalOperationConfirmationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Base wizard panel providing a reusable flow for simulation tasks and results.
 * <p>
 * This panel manages navigation between:
 * <ul>
 *     <li>Simulation task list</li>
 *     <li>Simulation result overview</li>
 *     <li>Simulation result objects</li>
 *     <li>Simulation result object details</li>
 * </ul>
 *
 * @param <C> Type of container object processed in the wizard.
 */
public abstract class SimulationWizardPanel<C extends Containerable> extends AbstractWizardPanel<C, ResourceDetailsModel> {

    public SimulationWizardPanel(String id, WizardPanelHelper<C, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    /**
     * Initializes the layout by showing the initial simulation tasks panel.
     */
    protected void initLayout() {
        add(createChoiceFragment(buildSimulationTasksPanel(getIdOfChoicePanel())));
    }

    /**
     * Builds the panel that shows simulation tasks.
     *
     * @param idOfChoicePanel Wicket component ID
     * @return the task wizard panel
     */
    private @NotNull ResourceSimulationTaskWizardPanel<C> buildSimulationTasksPanel(
            @NotNull String idOfChoicePanel) {
        PageBase pageBase = getPageBase();
        return new ResourceSimulationTaskWizardPanel<>(idOfChoicePanel, getHelper()) {
            @Override
            protected boolean isRefColumnVisible() {
                return false;
            }

            @Override
            protected void onShowSimulationResult(AjaxRequestTarget target, String resultOid) {
                if (!hasUnsavedChanges()) {
                    SimulationResultType simulationResultType = loadSimulationResult(pageBase, resultOid);
                    showChoiceFragment(target,
                            buildSimulationResultPanel(idOfChoicePanel, () -> simulationResultType));
                    return;
                }

                AdditionalOperationConfirmationPanel confirmationPanel = new AdditionalOperationConfirmationPanel(
                        pageBase.getMainPopupBodyId(),
                        createStringResource("SimulationTaskWizardPanel.unsavedChanges.message")) {

                    @Override
                    protected void performOnProcess(AjaxRequestTarget target) {
                        getHelper().onSaveObjectPerformed(target);
                        SimulationResultType simulationResultType = loadSimulationResult(getPageBase(), resultOid);
                        showChoiceFragment(target,
                                buildSimulationResultPanel(idOfChoicePanel, () -> simulationResultType));
                    }

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        SimulationResultType simulationResultType = loadSimulationResult(getPageBase(), resultOid);
                        showChoiceFragment(target,
                                buildSimulationResultPanel(idOfChoicePanel, () -> simulationResultType));
                    }
                };

                pageBase.showMainPopup(confirmationPanel, target);
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                SimulationWizardPanel.this.onBackPerformed(target);
            }

            @Override
            protected boolean isExitButtonVisible() {
                return false;
            }

            @Override
            protected IModel<String> getBackLabel() {
                return createStringResource("SimulationWizardPanel.back");
            }
        };
    }

    /**
     * Builds the panel that shows details of a simulation result.
     *
     * @param idOfChoicePanel Wicket component ID
     * @param model model of the simulation result
     * @return the result wizard panel
     */
    private @NotNull ResourceSimulationResultWizardPanel buildSimulationResultPanel(
            @NotNull String idOfChoicePanel, IModel<SimulationResultType> model) {
        return new ResourceSimulationResultWizardPanel(idOfChoicePanel, getAssignmentHolderModel(), model) {
            @Override
            protected void navigateToSimulationTasksWizard(
                    @NotNull String resultOid,
                    @Nullable ObjectReferenceType ref,
                    @Nullable ObjectProcessingStateType state,
                    @NotNull AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, buildSimulationObjectsResultPanel(idOfChoicePanel, model, state));
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, buildSimulationTasksPanel(idOfChoicePanel));
            }

            @Override
            protected boolean isExitButtonVisible() {
                return false;
            }

            @Override
            protected IModel<String> getBackLabel() {
                return createStringResource("SimulationTaskWizardPanel.correlationWizardPanel.back");
            }
        };
    }

    /**
     * Builds the panel that shows all objects within a simulation result.
     *
     * @param idOfChoicePanel Wicket component ID
     * @param model model of the simulation result
     * @param state optional processing state filter
     * @return the objects wizard panel
     */
    private @NotNull ResourceSimulationResultObjectsWizardPanel buildSimulationObjectsResultPanel(
            @NotNull String idOfChoicePanel, IModel<SimulationResultType> model,
            @Nullable ObjectProcessingStateType state) {
        return new ResourceSimulationResultObjectsWizardPanel(idOfChoicePanel, getAssignmentHolderModel(),
                model, state) {

            @Override
            protected void navigateToSimulationResultObject(
                    @NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target,
                        buildSimulationObjectResultPanel(idOfChoicePanel, model, object.getId(), markOid, state));
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, buildSimulationResultPanel(idOfChoicePanel, model));
            }

            @Override
            protected boolean isExitButtonVisible() {
                return false;
            }

            @Override
            protected IModel<String> getBackLabel() {
                return createStringResource(
                        "ResourceSimulationResultObjectsWizardPanel.simulationWizardPanel.back");
            }
        };
    }

    /**
     * Builds the panel that shows details of a single processed object
     * within a simulation result.
     *
     * @param idOfChoicePanel Wicket component ID
     * @param model model of the simulation result
     * @param simulationResultProcessedObjectId id of the processed object
     * @param markOid optional mark OID
     * @param state optional processing state
     * @return the object wizard panel
     */
    private @NotNull ResourceSimulationResultObjectWizardPanel buildSimulationObjectResultPanel(
            @NotNull String idOfChoicePanel,
            @NotNull IModel<SimulationResultType> model,
            @Nullable Long simulationResultProcessedObjectId,
            @Nullable String markOid,
            @Nullable ObjectProcessingStateType state) {
        LoadableDetachableModel<SimulationResultProcessedObjectType> prModel = loadSimulationResultProcessedObjectModel(
                getPageBase(),
                model.getObject().getOid(), simulationResultProcessedObjectId);

        return new ResourceSimulationResultObjectWizardPanel(idOfChoicePanel, getAssignmentHolderModel(),
                model, prModel, () -> markOid) {

            @Override
            protected void navigateToSimulationResultObject(@NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, buildSimulationObjectResultPanel(idOfChoicePanel,
                        model, object.getId(), markOid, state));
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, buildSimulationObjectsResultPanel(idOfChoicePanel, model, state));
            }

            @Override
            protected boolean isExitButtonVisible() {
                return false;
            }

            @Override
            protected IModel<String> getBackLabel() {
                return createStringResource(
                        "ResourceSimulationResultObjectWizardPanel.objectsWizardPanel.back");
            }
        };
    }

    /**
     * Called when the wizard exit is performed.
     *
     * @param target current Ajax request target
     */
    @Override
    protected void onExitPerformed(AjaxRequestTarget target) {
        // Currently we don't want to have "exit" in simulation wizard.
    }

    /**
     * Called when the "Back" action is triggered from the tasks panel.
     *
     * @param target current Ajax request target
     */
    public abstract void onBackPerformed(AjaxRequestTarget target);

}
