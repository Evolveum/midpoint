/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.log.OperationCompoundLogPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.log.OperationLogProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;

/**
 * Log viewer drawer entry showing logs produced by connector operations.
 */
public class OperationLogCollapsedItem extends CollapsedItem<WizardModelWithParentSteps> {

    @Serial private static final long serialVersionUID = 1L;

    private final Map<String, OperationLogProvider> providers = new HashMap<>();

    @Override
    public IModel<String> getIcon() {
        return Model.of("fa-solid fa-file-lines");
    }

    @Override
    public IModel<String> getTitle() {
        return PageBase.createStringResourceStatic("OperationLogCollapsedItem.title");
    }

    public List<OperationLogProvider> getProviders() {
        return providers.values().stream().toList();
    }

    public void addLogs(String panelId, OperationLogProvider provider) {
        providers.put(panelId, provider);
    }

    public void removeLogs(String panelId) {
        providers.remove(panelId);
    }

    @Override
    public int countOfObject() {
        return providers.values().stream()
                .mapToInt(provider -> provider.getOperationLogEntries().size())
                .sum();
    }

    @Override
    public boolean isVisible() {
        return providers.values().stream()
                .anyMatch(provider -> !provider.getOperationLogEntries().isEmpty());
    }

    @Override
    public Component getPanel(String id, WizardModelWithParentSteps wizardModel) {
        return new OperationCompoundLogPanel(id, Model.of(this), wizardModel);
    }
}
