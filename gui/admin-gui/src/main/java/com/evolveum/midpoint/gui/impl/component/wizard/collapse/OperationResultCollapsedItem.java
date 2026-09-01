/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperationResultCollapsedItem extends CollapsedItem<WizardModelWithParentSteps> {

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
        addOperationResult(panelId, fixPanelId, result, null);
    }

    public void addOperationResult(String panelId, String fixPanelId, OperationResult result, SerializableConsumer<AjaxRequestTarget> fixAction) {
        addOperationResult(panelId, fixPanelId, result, fixAction, null, null);
    }

    /** @see OperationResultWrapper#OperationResultWrapper(OperationResult, String, SerializableConsumer, String, String) */
    public void addOperationResult(
            String panelId, String fixPanelId, OperationResult result, SerializableConsumer<AjaxRequestTarget> fixAction,
            String fixButtonLabelKey, String fixButtonIcon) {
        removeOperationResult(panelId);
        OperationResultWrapper resultWrapper = new OperationResultWrapper(result, fixPanelId, fixAction, fixButtonLabelKey, fixButtonIcon);
        results.put(panelId, resultWrapper);
    }

    public void removeOperationResult(String panelId) {
        if (results.containsKey(panelId)) {
            results.remove(panelId);
        }
    }

    /** Removes every entry whose panelId starts with {@code prefix} (e.g. {@code "<stepId>."}). */
    public void removeOperationResultsByPrefix(String prefix) {
        results.keySet().removeIf(panelId -> panelId.startsWith(prefix));
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
