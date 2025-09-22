/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WizardModelWithParentSteps extends WizardModel {

    private int activeParentStepIndex;

    private int lastActiveParentStepIndex;
    private int lastActiveStepIndex;

    private Map<String, List<WizardStep>> childrenSteps = new HashMap<>();

    public WizardModelWithParentSteps(@NotNull List<WizardParentStep> steps) {
        super(steps);
    }

    public void init(Page page) {
        getSteps().forEach(parentStep -> {
            parentStep.init(this);
            getChildrenSteps((WizardParentStep) parentStep).forEach(s -> s.init(this));
        });

        String stepId = getStepIdFromParams(page);
        if (stepId != null) {
            setActiveStepById(stepId);
        } else {
            for (int i = 0; i < getSteps().size(); i++) {
                WizardParentStep parentStep = (WizardParentStep) getSteps().get(i);

                if (BooleanUtils.isTrue(parentStep.isStepVisible().getObject())) {
                    setActiveParentStepIndex(i);
                    List<WizardStep> childrenSteps = getChildrenSteps((WizardParentStep) getSteps().get(i));
                    if (childrenSteps.isEmpty()) {
                        setActiveStepIndex(-1);
                        break;
                    }
                    for (WizardStep step : childrenSteps) {
                        if (BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                            setActiveStepIndex(childrenSteps.indexOf(step));
                            break;
                        }
                    }
                    break;
                }
            }
        }

        fireActiveStepChanged(getActiveStep());
    }

    private List<WizardStep> getChildrenSteps(WizardParentStep parentStep) {
        if (!childrenSteps.containsKey(parentStep.getStepId())) {
            String defaultKey = parentStep.getDefaultStepId();
            if (childrenSteps.containsKey(defaultKey)) {
                childrenSteps.put(parentStep.getStepId(), childrenSteps.get(defaultKey));
                childrenSteps.remove(defaultKey);
            } else {
                childrenSteps.put(parentStep.getStepId(), parentStep.createChildrenSteps());
            }
        }
        return childrenSteps.get(parentStep.getStepId());
    }

    public void setActiveStepById(String id) {
        if (id == null) {
            return;
        }

        for (int i = 0; i < getSteps().size(); i++) {
            WizardParentStep parentStep = (WizardParentStep) getSteps().get(i);

            if (Objects.equals(id, parentStep.getStepId()) && BooleanUtils.isTrue(parentStep.isStepVisible().getObject())) {

                setActiveParentStepIndex(i);
                setActiveStepIndex(0);
                break;
            }
            boolean find = false;
            for (int in = 0; in < getChildrenSteps(parentStep).size(); in++) {
                WizardStep step = getChildrenSteps(parentStep).get(in);

                if (Objects.equals(id, step.getStepId()) && BooleanUtils.isTrue(step.isStepVisible().getObject())) {

                    setActiveParentStepIndex(i);
                    setActiveStepIndex(in);
                    find = true;
                    break;
                }
            }
            if (find) {
                break;
            }
        }
    }

    public int getActiveStepIndex() {
        return activeStepIndex;
//        WizardParentStep parentStep = (WizardParentStep) getSteps().get(activeParentStepIndex);
//        return findRealActiveIndex(parentStep.getChildrenSteps(), activeStepIndex);
    }

    public int getActiveParentStepIndex() {
        return activeParentStepIndex;
//        return findRealActiveIndex(getSteps(), activeParentStepIndex);
    }

    private int findRealActiveIndex(List<? extends WizardStep> steps, int activeIndex) {
        int index = 0;
        for (int i = 0; i < activeIndex; i++) {
            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                index++;
            }
        }
        return index;
    }

    public boolean hasNext() {
        return findNextStep(false) != null;
    }

    private WizardStep findNextStep(boolean storeResult) {
        WizardParentStep parentStep = (WizardParentStep) getSteps().get(activeParentStepIndex);
        List<WizardStep> activeChildrenSteps = getChildrenSteps(parentStep);
        for (int i = activeStepIndex + 1; i < activeChildrenSteps.size(); i++) {
            if (BooleanUtils.isTrue(activeChildrenSteps.get(i).isStepVisible().getObject())) {
                if (storeResult) {
                    setActiveStepIndex(i);
                }
                return activeChildrenSteps.get(i);
            }
        }

        if (activeParentStepIndex == getSteps().size()) {
            return null;
        }
        for (int pi = activeParentStepIndex + 1; pi < getSteps().size(); pi++) {
            if (BooleanUtils.isTrue(getSteps().get(pi).isStepVisible().getObject())) {
                parentStep = (WizardParentStep) getSteps().get(pi);
                List<WizardStep> childrenSteps = getChildrenSteps(parentStep);
                if (childrenSteps.isEmpty()) {
                    if (storeResult) {
                        setActiveStepIndex(-1);
                        setActiveParentStepIndex(pi);
                    }
                    return parentStep;
                }
                for (WizardStep step : childrenSteps) {
                    if (BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                        if (storeResult) {
                            setActiveStepIndex(childrenSteps.indexOf(step));
                            setActiveParentStepIndex(pi);
                        }
                        return step;
                    }
                }
            }
        }

        return null;
    }

    public void next() {
        int index = activeStepIndex;
        int parentIndex = activeParentStepIndex;

        findNextStep(true);

        if (index != activeStepIndex || parentIndex != activeParentStepIndex) {
            fireActiveStepChanged(getActiveStep());
        }
    }

    public boolean hasPrevious() {
        return findPreviousStep(false) != null;
    }

    private WizardStep findPreviousStep(boolean storeResult) {
        WizardParentStep parentStep = (WizardParentStep) getSteps().get(activeParentStepIndex);
        List<WizardStep> activeChildrenSteps = getChildrenSteps(parentStep);
        for (int i = activeStepIndex - 1; i >= -1; i--) {
            if (i < 0) {
                break;
            } else if (BooleanUtils.isTrue(activeChildrenSteps.get(i).isStepVisible().getObject())) {
                if (storeResult) {
                    setActiveStepIndex(i);
                }
                return activeChildrenSteps.get(i);
            }
        }

        if (activeParentStepIndex == 0) {
            return null;
        }
        for (int pi = activeParentStepIndex - 1; pi >= 0; pi--) {
            if (BooleanUtils.isTrue(getSteps().get(pi).isStepVisible().getObject())) {
                parentStep = (WizardParentStep) getSteps().get(pi);
                List<WizardStep> childrenSteps = getChildrenSteps(parentStep);
                if (childrenSteps.isEmpty()) {
                    if (storeResult) {
                        setActiveStepIndex(-1);
                        setActiveParentStepIndex(pi);
                    }
                    return parentStep;
                }
                for (int pchi = childrenSteps.size() - 1; pchi >= 0; pchi--) {
                    if (BooleanUtils.isTrue(childrenSteps.get(pchi).isStepVisible().getObject())) {
                        if (storeResult) {
                            setActiveStepIndex(pchi);
                            setActiveParentStepIndex(pi);
                        }
                        return childrenSteps.get(pchi);
                    }
                }
            }
        }

        return null;
    }

    public void previous() {
        int index = activeStepIndex;
        int parentIndex = activeParentStepIndex;

        findPreviousStep(true);

        if (index != activeStepIndex || parentIndex != activeParentStepIndex) {
            fireActiveStepChanged(getActiveStep());
        }
    }

    public WizardStep getNextPanel() {
        return findNextStep(false);
    }

    private void setActiveParentStepIndex(int activeParentStepIndex) {
        if (this.lastActiveParentStepIndex < activeParentStepIndex) {
            lastActiveParentStepIndex = activeParentStepIndex;
        }
        this.activeParentStepIndex = activeParentStepIndex;
    }

    @Override
    protected void setActiveStepIndex(int activeStepIndex) {
        if (this.lastActiveStepIndex < activeStepIndex) {
            lastActiveStepIndex = activeStepIndex;
        }
        super.setActiveStepIndex(activeStepIndex);
    }

    public WizardStep getActiveStep() {
        if (activeStepIndex == -1) {
            return getSteps().get(activeParentStepIndex);
        }
        return getActiveChildrenSteps().get(activeStepIndex);
    }

    public List<WizardStep> getActiveChildrenSteps() {
        return getChildrenSteps(getActiveParentStep());
    }

    public WizardParentStep getActiveParentStep() {
        return ((WizardParentStep) getSteps().get(activeParentStepIndex));
    }

    public void setActiveChildStepById(String id) {
        if (id == null) {
            return;
        }

        for (int i = 0; i < getSteps().size(); i++) {
            WizardStep step = getSteps().get(i);

            if (Objects.equals(id, step.getStepId()) && BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                setActiveParentStepIndex(i);
                setActiveStepIndex(0);
                break;
            }
        }
    }

    protected void initNewStep(WizardStep newStep) {
        newStep.init(this);
        if (newStep instanceof WizardParentStep parentStep) {
            getChildrenSteps(parentStep).forEach(s -> s.init(this));
        }
    }
}
