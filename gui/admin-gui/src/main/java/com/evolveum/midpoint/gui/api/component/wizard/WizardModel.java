/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.util.List;

import org.apache.wicket.util.io.IClusterable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardModel implements IClusterable {

    private static final long serialVersionUID = 1L;

    private WizardPanel panel;

    private List<WizardStep> steps;
    private int activeStepIndex;

    public WizardModel(@NotNull List<WizardStep> steps) {
        this.steps = steps;
    }

    public void init() {
        steps.forEach(s -> s.init(this));
    }

    public WizardPanel getPanel() {
        return panel;
    }

    public void setPanel(WizardPanel panel) {
        this.panel = panel;
    }

    public List<WizardStep> getSteps() {
        return steps;
    }

    public WizardStep getActiveStep() {
        return steps.get(activeStepIndex);
    }

    public int getActiveStepIndex() {
        return activeStepIndex;
    }

    private void setActiveStepIndex(int activeStepIndex) {
        if (activeStepIndex < 0) {
            return;
        }
        if (activeStepIndex >= steps.size()) {
            return;
        }

        this.activeStepIndex = activeStepIndex;
    }

    public void next() {
        setActiveStepIndex(activeStepIndex + 1);
    }

    public void previous() {
        setActiveStepIndex(activeStepIndex - 1);
    }

    public WizardStep getNextPanel() {
        int nextIndex = activeStepIndex + 1;
        if (steps.size() <= nextIndex) {
            return null;
        }

        return steps.get(nextIndex);
    }
}
