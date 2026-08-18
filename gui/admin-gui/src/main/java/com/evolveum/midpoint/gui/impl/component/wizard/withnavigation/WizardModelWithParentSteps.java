/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.withnavigation;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;

import com.evolveum.midpoint.gui.impl.component.wizard.collapse.CollapsedItem;

import com.evolveum.midpoint.gui.impl.component.wizard.collapse.DrawerDescriptor;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.OperationResultCollapsedItem;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.OperationResultWrapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class WizardModelWithParentSteps extends WizardModel implements DrawerDescriptor {

    private final OperationResultCollapsedItem operationResultCollapsedItem = new OperationResultCollapsedItem();

    public abstract void init(Page page);

    public abstract List<WizardParentStep> getAllParentSteps();

    public abstract int getActiveParentStepIndex();

    public abstract int getInProgressParentStepIndex();

    public abstract int getInProgressStepIndex();

    public abstract List<WizardStep> getActiveChildrenSteps();

    public abstract List<WizardStep> getInProgressChildrenSteps();

    public abstract WizardParentStep getActiveParentStep();

    public IModel<List<CollapsedItem>> getCollapsedItems() {
        return Model.ofList(getCollapsedItemsList());
    }

    private @NotNull List<CollapsedItem> getCollapsedItemsList() {
        return List.of(operationResultCollapsedItem);
    }

    public boolean isCollapsedItemsVisible() {
        return operationResultCollapsedItem.isVisible();
    }

    public Optional<CollapsedItem> getSelectedCollapsedItem() {
        return getCollapsedItemsList().stream()
                .filter(CollapsedItem::isSelected).findFirst();
    }

    @Override
    public void fireActiveStepChanged(WizardStep step) {
        super.fireActiveStepChanged(step);
        removeOperationResult(step.getStepId());
        getCollapsedItemsList().forEach(item -> item.setSelected(false));
    }

    public void addOperationResult(String panelId, OperationResult result) {
        addOperationResult(panelId, null, result);
    }

    public void addOperationResult(String panelId, String fixPanelId, OperationResult result) {
        operationResultCollapsedItem.addOperationResult(panelId, fixPanelId, result);
    }

    public void addOperationResult(String panelId, OperationResult result, SerializableConsumer<AjaxRequestTarget> fixAction) {
        operationResultCollapsedItem.addOperationResult(panelId, null, result, fixAction);
    }

    /**
     * Adds an entry whose fix button is repurposed for a different action than step navigation
     * (e.g. disabling a broken sibling script's manifest entry instead of "fixing" it), with its
     * own label/icon instead of the default "Fix it" - see {@link OperationResultWrapper}.
     */
    public void addOperationResult(
            String panelId, OperationResult result, SerializableConsumer<AjaxRequestTarget> fixAction,
            String fixButtonLabelKey, String fixButtonIcon) {
        operationResultCollapsedItem.addOperationResult(panelId, null, result, fixAction, fixButtonLabelKey, fixButtonIcon);
    }

    public void removeOperationResult(String panelId) {
        operationResultCollapsedItem.removeOperationResult(panelId);
    }

    /** Removes every drawer entry whose panelId starts with {@code prefix} (e.g. {@code "<stepId>."}). */
    public void removeOperationResultsByPrefix(String prefix) {
        operationResultCollapsedItem.removeOperationResultsByPrefix(prefix);
    }

    public boolean isStepWithError(String stepId) {
        if (StringUtils.isEmpty(stepId)) {
            return false;
        }
        return operationResultCollapsedItem.getResults().stream()
                .anyMatch(operationResultWrapper -> Strings.CS.equals(stepId, operationResultWrapper.getFixPanelId()));
    }

    public List<OperationResult> getOperationResultsForFixStep(String stepId) {
        if (StringUtils.isEmpty(stepId)) {
            return List.of();
        }
        return operationResultCollapsedItem.getResults().stream()
                .filter(operationResultWrapper -> Strings.CS.equals(stepId, operationResultWrapper.getFixPanelId()))
                .map(OperationResultWrapper::getResult)
                .toList();
    }

    public abstract boolean isShowedSummary();

    public abstract void showSummaryPanel();

    /**
     * Forces the cached children steps of the given parent step to be recomputed on next access
     * (e.g. after a decision that {@link WizardParentStep#createChildrenSteps()} depends on, such
     * as an integration type selection, has just been persisted).
     */
    public abstract void invalidateChildrenSteps(String parentStepId);

//    public AbstractWizardController getWizardController() {
//        return wizardController;
//    }
}
