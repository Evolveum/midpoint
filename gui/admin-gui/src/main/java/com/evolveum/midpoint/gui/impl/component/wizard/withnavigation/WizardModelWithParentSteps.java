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

import com.evolveum.midpoint.gui.impl.component.wizard.collapse.OperationResultCollapsedItem;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class WizardModelWithParentSteps extends WizardModel {

    private final OperationResultCollapsedItem operationResultCollapsedItem = new OperationResultCollapsedItem();

    public void init(Page page) {
        fireActiveStepChanged(getActiveStep());
    }

    public abstract List<WizardParentStep> getAllParentSteps();

    public abstract int getActiveParentStepIndex();

    public abstract List<WizardStep> getActiveChildrenSteps();

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

    public void removeOperationResult(String panelId) {
        operationResultCollapsedItem.removeOperationResult(panelId);
    }

//    public AbstractWizardController getWizardController() {
//        return wizardController;
//    }
}
