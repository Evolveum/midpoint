/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.*;

public class OperationResultCollapsedItem extends CollapsedItem {

    private Map<String, OperationResultWrapper> results = new HashMap<>();

    @Override
    public IModel<String> getIcon() {
        return Model.of("fa fa-exclamation-triangle");
    }

    @Override
    public IModel<String> getTitle() {
        return PageBase.createStringResourceStatic("OperationResultCollapsedItem.title");
    }

    public List<OperationResultWrapper> getResults() {
        return results.values().stream().toList();
    }

    public void addOperationResult(String panelId, String fixPanelId, OperationResult result) {
        removeOperationResult(panelId);
        OperationResultWrapper resultWrapper = new OperationResultWrapper(result, fixPanelId);
        results.put(panelId, resultWrapper);
    }

    public void removeOperationResult(String panelId) {
        if (results.containsKey(panelId)) {
            results.remove(panelId);
        }
    }

    @Override
    public int countOfObject() {
        return getResults().size();
    }

    @Override
    public boolean isVisible() {
        return !getResults().isEmpty();
    }

    @Override
    public Component getPanel(String id, WizardModelWithParentSteps wizardModel) {
        return new OperationResultCollapsedItemPanel(id, Model.of(this), wizardModel);
    }
}
