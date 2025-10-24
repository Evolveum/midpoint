/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;

import org.apache.wicket.model.IModel;

public class WizardHeaderStepHelper {

    int activeStepIndex;
    int totalStepsCount;
    WizardPanel parentPanel;

    public WizardHeaderStepHelper(int activeStepIndex, int totalStepsCount, WizardPanel parentPanel) {
        this.activeStepIndex = activeStepIndex;
        this.totalStepsCount = totalStepsCount;
        this.parentPanel = parentPanel;
    }

    public boolean isStepActive(int stepIndex) {
        return stepIndex == activeStepIndex;
    }

    public IModel<String> getStepPanelAriaLabelModel(int stepIndex, String stepName) {
        return isStepActive(stepIndex) ?
                parentPanel.createStringResource("WizardPanel.step.selected.ariaLabel", getIndexToAnnounce(stepIndex),
                        totalStepsCount, stepName)
                : parentPanel.createStringResource("WizardPanel.step.notSelected.ariaLabel",
                getIndexToAnnounce(stepIndex), totalStepsCount, stepName);
    }

    private int getIndexToAnnounce(int index) {
        return index + 1;
    }
}
