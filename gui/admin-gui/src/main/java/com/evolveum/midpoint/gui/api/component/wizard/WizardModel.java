/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class WizardModel implements IClusterable {
    private static final long serialVersionUID = 1L;

    public static final String PARAM_STEP = "step";

    private List<WizardListener> wizardListeners = new ArrayList<>();

    private Component panel;

    public void addWizardListener(@NotNull WizardListener listener) {
        wizardListeners.add(listener);
    }

    public void removeWizardListener(@NotNull WizardListener listener) {
        wizardListeners.remove(listener);
    }

    public void fireActiveStepChanged(final WizardStep step) {
        wizardListeners.forEach(listener -> listener.onStepChanged(step));
    }

    public final void fireActiveStepChanged() {
        WizardStep step = getActiveStep();
        fireActiveStepChanged(step);
    }

    protected final void fireWizardCancelled() {
        wizardListeners.forEach(listener -> listener.onCancel());
    }

    protected final void fireWizardFinished() {
        wizardListeners.forEach(listener -> listener.onFinish());
    }

    public abstract void init(Page page);

    public final String getStepIdFromParams(Page page) {
        if (page == null) {
            return null;
        }

        PageParameters params = page.getPageParameters();
        if (params == null) {
            return null;
        }

        StringValue step = params.get(PARAM_STEP);
        return step != null ? step.toString() : null;
    }

    public final Component getPanel() {
        return panel;
    }

    public final void setPanel(Component panel) {
        this.panel = panel;
    }

    public abstract List<? extends WizardStep> getSteps();

    public abstract WizardStep getActiveStep();

    public abstract void setActiveStepById(String id);

    public abstract int getActiveStepIndex();

    public abstract boolean hasNext();

    public abstract void next();

    public abstract boolean hasPrevious();

    public abstract WizardStep findPreviousStep();

    public abstract void previous();

    public abstract WizardStep getNextPanel();
}
