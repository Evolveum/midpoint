/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassConnectorStepPanel;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardModel implements IClusterable {

    private static final long serialVersionUID = 1L;

    public static final String PARAM_STEP = "step";

    private List<WizardListener> wizardListeners = new ArrayList<>();

    private WizardPanel panel;

    private List<WizardStep> steps;
    protected int activeStepIndex;

    public WizardModel(List<? extends WizardStep> steps) {
        this.steps = (List<WizardStep>) steps;
    }

    public void addWizardListener(@NotNull WizardListener listener) {
        wizardListeners.add(listener);
    }

    public void removeWizardListener(@NotNull WizardListener listener) {
        wizardListeners.remove(listener);
    }

    public final void fireActiveStepChanged(final WizardStep step) {
        wizardListeners.forEach(listener -> listener.onStepChanged(step));
    }

    public final void fireActiveStepChanged() {
        WizardStep step = getActiveStep();
        wizardListeners.forEach(listener -> listener.onStepChanged(step));
    }

    protected final void fireWizardCancelled() {
        wizardListeners.forEach(listener -> listener.onCancel());
    }

    protected final void fireWizardFinished() {
        wizardListeners.forEach(listener -> listener.onFinish());
    }

    public void init(Page page) {
        steps.forEach(s -> s.init(this));

        String stepId = getStepIdFromParams(page);
        if (stepId != null) {
            setActiveStepById(stepId);
        } else {
            for (int i = 0; i < steps.size(); i++) {
                WizardStep step = steps.get(i);

                if (BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                    activeStepIndex = i;
                    break;
                }
            }
        }

        fireActiveStepChanged(getActiveStep());
    }

    protected final String getStepIdFromParams(Page page) {
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

    public Component getPanel() {
        return panel;
    }

    public Component getHeader() {
        return panel.getHeader();
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

    public void setActiveStepById(String id) {
        if (id == null) {
            return;
        }

        for (int i = 0; i < steps.size(); i++) {
            WizardStep step = steps.get(i);

            if (Objects.equals(id, step.getStepId()) && BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                activeStepIndex = i;
                break;
            }
        }
    }

    public int getActiveStepIndex() {
        int index = 0;
        for (int i = 0; i < activeStepIndex; i++) {
            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                index++;
            }
        }
        return index;
    }

    public boolean hasNext() {
        return findNextStep() != null;
    }

    private WizardStep findNextStep() {
        for (int i = activeStepIndex + 1; i < steps.size(); i++) {
            if (i >= steps.size()) {
                return null;
            }

            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                return steps.get(i);
            }
        }

        return null;
    }

    public void next() {
        int index = activeStepIndex;

        WizardStep next = findNextStep();
        if (next == null) {
            return;
        }

        activeStepIndex = steps.indexOf(next);

        if (index != activeStepIndex) {
            fireActiveStepChanged(getActiveStep());
        }
    }

    public boolean hasPrevious() {
        return findPreviousStep() != null;
    }

    public WizardStep findPreviousStep() {
        for (int i = activeStepIndex - 1; i >= 0; i--) {
            if (i < 0) {
                return null;
            }

            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                return steps.get(i);
            }
        }

        return null;
    }

    public void previous() {
        int index = activeStepIndex;

        WizardStep previous = findPreviousStep();
        if (previous == null) {
            return;
        }

        activeStepIndex = steps.indexOf(previous);

        if (index != activeStepIndex) {
            fireActiveStepChanged(getActiveStep());
        }
    }

    public WizardStep getNextPanel() {
        return findNextStep();
    }

    protected void setActiveStepIndex(int activeStepIndex) {
        this.activeStepIndex = activeStepIndex;
    }

    public void addStepAfter(WizardStep newStep, Class<?> objectClassConnectorStepPanelClass) {
        boolean find = false;
        for (WizardStep step : steps) {
            if (find && !objectClassConnectorStepPanelClass.equals(step.getClass())) {
                initNewStep(newStep);
                steps.add(steps.indexOf(step), newStep);
                return;
            }
            if (objectClassConnectorStepPanelClass.equals(step.getClass())) {
                find = true;
            }
        }
        steps.add(newStep);
    }

    protected void initNewStep(WizardStep newStep) {
        newStep.init(this);
    }

    public void addStepBefore(WizardStep newStep, Class<?> objectClassConnectorStepPanelClass) {
        for (WizardStep step : steps) {
            if (objectClassConnectorStepPanelClass.equals(step.getClass())) {
                initNewStep(newStep);
                steps.add(steps.indexOf(step), newStep);
                return;
            }
        }
        steps.add(newStep);
    }
}
