/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationResultObjectWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationResultObjectsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationResultWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationTaskWizardPanel;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.component.dialog.AdditionalOperationConfirmationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.loadSimulationResultProcessedObjectModel;

/**
 * @author lskublik
 */
public class CorrelationWizardPanel extends AbstractWizardPanel<CorrelationDefinitionType, ResourceDetailsModel> {

    public CorrelationWizardPanel(String id, WizardPanelHelper<CorrelationDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected CorrelationItemsTableWizardPanel createTablePanel() {
        return new CorrelationItemsTableWizardPanel(getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void redirectToSimulationTasksWizard(AjaxRequestTarget target) {
                showChoiceFragment(target, buildSimulationTasksPanel(getIdOfChoicePanel()));
            }

            @Override
            protected void postProcessAddSuggestion(AjaxRequestTarget target) {
                showUnsavedChangesToast(target);
                showChoiceFragment(target, createTablePanel());
            }

            @Override
            protected void showTableForItemRefs(
                    @NotNull AjaxRequestTarget target,
                    @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
                    @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    @Nullable StatusInfo<CorrelationSuggestionsType> statusInfo) {
                WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> helper = new WizardPanelHelper<>(
                        getAssignmentHolderDetailsModel(), rowModel) {
                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        showUnsavedChangesToast(target);
                        showChoiceFragment(target, createTablePanel());
                    }
                };

                showChoiceFragment(target, new CorrelationItemRuleWizardPanel(getIdOfChoicePanel(), helper, () -> statusInfo) {
                    @Override
                    protected void acceptSuggestionPerformed(
                            @NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel) {
                        acceptSuggestionItemPerformed(getPageBase(), target, valueModel, resourceObjectTypeDefinition, statusInfo);
                    }

                    @Override
                    protected void onDiscardButtonClick(
                            @NotNull PageBase pageBase,
                            @NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
                            @NotNull StatusInfo<?> statusInfo) {
                        performDiscard(pageBase, target, valueModel, statusInfo);
                    }

                    @Override
                    protected boolean isShowEmptyField() {
                        return true;
                    }

                });
            }
        };
    }

    private @NotNull ResourceSimulationTaskWizardPanel<CorrelationDefinitionType> buildSimulationTasksPanel(
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
                    SimulationResultType simulationResultType = SimulationsGuiUtil.loadSimulationResult(getPageBase(), resultOid);
                    showChoiceFragment(target, buildSimulationResultPanel(idOfChoicePanel, () -> simulationResultType));
                }

                AdditionalOperationConfirmationPanel confirmationPanel = new AdditionalOperationConfirmationPanel(
                        pageBase.getMainPopupBodyId(),
                        createStringResource("SimulationTaskWizardPanel.redirect.possibilityToLost.unsavedChanges.message")) {

                    @Override
                    protected void performOnProcess(AjaxRequestTarget target) {
                        getHelper().onSaveObjectPerformed(target);
                        SimulationResultType simulationResultType = SimulationsGuiUtil.loadSimulationResult(getPageBase(), resultOid);
                        showChoiceFragment(target, buildSimulationResultPanel(idOfChoicePanel, () -> simulationResultType));
                    }

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        SimulationResultType simulationResultType = SimulationsGuiUtil.loadSimulationResult(getPageBase(), resultOid);
                        showChoiceFragment(target, buildSimulationResultPanel(idOfChoicePanel, () -> simulationResultType));
                    }
                };

                pageBase.showMainPopup(confirmationPanel, target);
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, createTablePanel());
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

    private @NotNull ResourceSimulationResultObjectsWizardPanel buildSimulationObjectsResultPanel(
            @NotNull String idOfChoicePanel, IModel<SimulationResultType> model,
            @Nullable ObjectProcessingStateType state) {
        return new ResourceSimulationResultObjectsWizardPanel(idOfChoicePanel, getAssignmentHolderModel(), model, state) {

            @Override
            protected void navigateToSimulationResultObject(
                    @NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, buildSimulationObjectResultPanel(idOfChoicePanel, model, object.getId(), markOid, state));
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
                return createStringResource("ResourceSimulationResultObjectsWizardPanel.simulationWizardPanel.back");
            }
        };
    }

    private @NotNull ResourceSimulationResultObjectWizardPanel buildSimulationObjectResultPanel(
            @NotNull String idOfChoicePanel,
            @NotNull IModel<SimulationResultType> model,
            @Nullable Long simulationResultProcessedObjectId,
            @Nullable String markOid,
            @Nullable ObjectProcessingStateType state) {
        LoadableDetachableModel<SimulationResultProcessedObjectType> prModel = loadSimulationResultProcessedObjectModel(
                getPageBase(),
                model.getObject().getOid(), simulationResultProcessedObjectId);

        return new ResourceSimulationResultObjectWizardPanel(idOfChoicePanel, getAssignmentHolderModel(), model, prModel, () -> markOid) {

            @Override
            protected void navigateToSimulationResultObject(@NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, buildSimulationObjectResultPanel(idOfChoicePanel, model, object.getId(), markOid, state));
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
                return createStringResource("ResourceSimulationResultObjectWizardPanel.objectsWizardPanel.back");
            }
        };
    }
}
