/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardModelBasic extends WizardModel {

    private static final long serialVersionUID = 1L;

    public static final String PARAM_STEP = "step";

    private List<WizardStep> steps;
    protected int activeStepIndex;

    public WizardModelBasic(List<? extends WizardStep> steps) {
        this.steps = (List<WizardStep>) steps;
    }

    @Override
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

    public List<? extends WizardStep> getSteps() {
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

    public final Component getHeader() {
        return ((WizardPanel)getPanel()).getHeader();
    }
}
