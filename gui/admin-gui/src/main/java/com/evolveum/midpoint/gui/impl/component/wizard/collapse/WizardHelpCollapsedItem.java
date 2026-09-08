/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardStepPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;

/**
 * Context aware documentation for the active wizard step, shown in the wizard drawer next to the
 * operation results.
 *
 */
public class WizardHelpCollapsedItem extends CollapsedItem<WizardModelWithParentSteps> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ICON = "fa fa-info-circle text-primary";
    private static final String KEY_TITLE = "WizardHelpCollapsedItem.title";

    private final WizardModelWithParentSteps wizardModel;

    public WizardHelpCollapsedItem(WizardModelWithParentSteps wizardModel) {
        this.wizardModel = wizardModel;
    }

    @Override
    public @NotNull IModel<String> getIcon() {
        return Model.of(ICON);
    }

    @Override
    public IModel<String> getTitle() {
        return () -> LocalizationUtil.translate(KEY_TITLE);
    }

    @Override
    public @NotNull Component getPanel(String id, WizardModelWithParentSteps drawerModel) {
        return new HelpContentPanel(id, Model.of(new HelpContentModel(getTabs())));
    }

    @Override
    public boolean isVisible() {
        return !getTabs().isEmpty();
    }

    private @NotNull List<HelpTab> getTabs() {
        WizardStep step = wizardModel.getActiveStep();
        if (step instanceof BasicWizardStepPanel<?> stepPanel) {
            List<HelpTab> tabs = stepPanel.getHelpTabs();
            if (tabs != null) {
                return tabs;
            }
        }
        return List.of();
    }
}
