/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.withnavigation;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Page;
import org.apache.wicket.util.io.IClusterable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractWizardPartItem<AH extends AssignmentHolderType, ADM extends AssignmentHolderDetailsModel<AH>> implements IClusterable {

    private final WizardPanelHelper<? extends Containerable, ADM> helper;

    private List<WizardParentStep> parentSteps;
    private List<WizardStep> steps;
    private final Map<String, List<WizardStep>> childrenSteps = new HashMap<>();

    private int activeParentStepIndex;
    private int activeStepIndex;

    private int inProgressParentStepIndex = -1;
    private int inProgressStepIndex = -1;

    private int lastActiveParentStepIndex = -1;
    private int lastActiveStepIndex = -1;

    private AbstractWizardController<AH, ADM> controller;

    private String parameter;

    protected AbstractWizardPartItem(WizardPanelHelper<? extends Containerable, ADM> helper) {
        this.helper = helper;
    }

    public final WizardPanelHelper<? extends Containerable, ADM> getHelper() {
        return helper;
    }

    protected final ADM getObjectDetailsModel() {
        return helper.getDetailsModel();
    }

    protected final PrismObjectWrapper<AH> getObjectWrapper() {
        return getObjectDetailsModel().getObjectWrapper();
    }

    @Nullable
    protected String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public abstract boolean isComplete();

    public void init(Page page, AbstractWizardController<AH, ADM> controller) {
        this.controller = controller;
        childrenSteps.clear();
        this.parentSteps = createWizardSteps();
        getParentSteps().forEach(parentStep -> {
            parentStep.init(this.controller);
            getChildrenSteps(parentStep).forEach(s -> s.init(this.controller));
        });

        String stepId = this.controller.getStepIdFromParams(page);
        if (stepId != null) {
            setActiveStepById(stepId);
        } else {
            for (int i = 0; i < getParentSteps().size(); i++) {
                WizardParentStep parentStep = getParentSteps().get(i);

                if (BooleanUtils.isTrue(parentStep.isStepVisible().getObject())) {

                    if (isParentComplete(parentStep)) {
                        continue;
                    }

                    setActiveParentStepIndex(i);
                    List<WizardStep> childrenSteps = getChildrenSteps(getParentSteps().get(i));
                    if (childrenSteps.isEmpty()) {
                        setActiveStepIndex(-1);
                        break;
                    }
                    for (WizardStep step : childrenSteps) {
                        if (BooleanUtils.isTrue(step.isStepVisible().getObject())) {

                            if (step.isCompleted()) {
                                continue;
                            }

                            setActiveStepIndex(childrenSteps.indexOf(step));
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private boolean isParentComplete(WizardParentStep parentStep) {
        List<WizardStep> activeChildrenSteps = getChildrenSteps(parentStep);
        for (int i = activeChildrenSteps.size() - 1; i >= -1; i--) {
            if (i < 0) {
                return parentStep.isCompleted();
            } else if (BooleanUtils.isTrue(activeChildrenSteps.get(i).isStepVisible().getObject())) {
                if (activeChildrenSteps.get(i).isStatusStep()) {
                    continue;
                }
                return activeChildrenSteps.get(i).isCompleted();
            }
        }
        return false;
    }

    protected abstract List<WizardParentStep> createWizardSteps();

    public boolean isInitialized() {
        return this.parentSteps != null;
    }

    public abstract Enum<?> getIdentifierForWizardStatus();

    public final List<WizardParentStep> getParentSteps() {
        return parentSteps;
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

    public boolean setActiveStepById(String id) {
        if (id == null) {
            return false;
        }

        for (int i = 0; i < getParentSteps().size(); i++) {
            WizardParentStep parentStep = getParentSteps().get(i);

            if (Objects.equals(id, parentStep.getStepId()) && BooleanUtils.isTrue(parentStep.isStepVisible().getObject())) {

                setActiveParentStepIndex(i);
                List<WizardStep> childrenSteps = getChildrenSteps(parentStep);
                if (childrenSteps.isEmpty()) {
                    setActiveStepIndex(-1);
                } else {
                    setActiveStepIndex(0);
                }
                return true;
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
                return true;
            }
        }
        return false;
    }

    public int getActiveStepIndex() {
        return activeStepIndex;
    }

    public int getActiveParentStepIndex() {
        return activeParentStepIndex;
    }

    public int getInProgressStepIndex() {
        return inProgressStepIndex;
    }

    public int getInProgressParentStepIndex() {
        return inProgressParentStepIndex;
    }

    public boolean hasNext() {
        return findNextStep(false) != null;
    }

    private WizardStep findNextStep(boolean storeResult) {
        WizardParentStep parentStep = getParentSteps().get(activeParentStepIndex);
        List<WizardStep> activeChildrenSteps = getChildrenSteps(parentStep);
        for (int i = activeStepIndex + 1; i < activeChildrenSteps.size(); i++) {
            if (BooleanUtils.isTrue(activeChildrenSteps.get(i).isStepVisible().getObject())) {
                if (storeResult) {
                    setActiveStepIndex(i);
                }
                return activeChildrenSteps.get(i);
            }
        }

        if (activeParentStepIndex == getParentSteps().size() - 1) {
            return null;
        }
        for (int pi = activeParentStepIndex + 1; pi < getParentSteps().size(); pi++) {
            if (BooleanUtils.isTrue(getParentSteps().get(pi).isStepVisible().getObject())) {
                parentStep = getParentSteps().get(pi);
                List<WizardStep> childrenSteps = getChildrenSteps(parentStep);
                if (childrenSteps.isEmpty()) {
                    if (storeResult) {
                        setActiveParentStepIndex(pi);
                        setActiveStepIndex(-1);
                    }
                    return parentStep;
                }
                for (WizardStep step : childrenSteps) {
                    if (BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                        if (storeResult) {
                            setActiveParentStepIndex(pi);
                            setActiveStepIndex(childrenSteps.indexOf(step));
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
        String oldPanelId = getActiveStep().getStepId();

        findNextStep(true);

        if (index != activeStepIndex || parentIndex != activeParentStepIndex) {
            controller.removeOperationResult(oldPanelId);
            controller.fireActiveStepChanged(getActiveStep());
        }
    }

    public boolean hasPrevious() {
        return findPreviousStep(false) != null;
    }

    public WizardStep findPreviousStep(boolean storeResult) {
        WizardParentStep parentStep = getParentSteps().get(activeParentStepIndex);
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
            if (BooleanUtils.isTrue(getParentSteps().get(pi).isStepVisible().getObject())) {
                parentStep = getParentSteps().get(pi);
                List<WizardStep> childrenSteps = getChildrenSteps(parentStep);
                if (childrenSteps.isEmpty()) {
                    if (storeResult) {
                        setActiveParentStepIndex(pi);
                        setActiveStepIndex(-1);
                    }
                    return parentStep;
                }
                for (int pchi = childrenSteps.size() - 1; pchi >= 0; pchi--) {
                    if (BooleanUtils.isTrue(childrenSteps.get(pchi).isStepVisible().getObject())) {
                        if (storeResult) {
                            setActiveParentStepIndex(pi);
                            setActiveStepIndex(pchi);
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
            controller.fireActiveStepChanged(getActiveStep());
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
        if (this.activeParentStepIndex > inProgressParentStepIndex) {
            inProgressParentStepIndex = this.activeParentStepIndex;
            inProgressStepIndex = -1;
        }
    }

    private void setActiveStepIndex(int activeStepIndex) {
        if (this.lastActiveStepIndex < activeStepIndex) {
            lastActiveStepIndex = activeStepIndex;
        }
        this.activeStepIndex = activeStepIndex;
        if (this.activeStepIndex > inProgressStepIndex) {
            inProgressStepIndex = this.activeStepIndex;
        }
    }

    public WizardStep getActiveStep() {
        if (activeStepIndex == -1) {
            return getParentSteps().get(activeParentStepIndex);
        }
        return getActiveChildrenSteps().get(activeStepIndex);
    }

    public List<WizardStep> getActiveChildrenSteps() {
        return getChildrenSteps(getActiveParentStep());
    }

    public WizardParentStep getActiveParentStep() {
        return getParentSteps().get(activeParentStepIndex);
    }

    public void setActiveParentStepById(String id) {
        if (id == null) {
            return;
        }

        for (int i = 0; i < getParentSteps().size(); i++) {
            WizardParentStep parentStep = getParentSteps().get(i);

            if (Objects.equals(id, parentStep.getStepId()) && BooleanUtils.isTrue(parentStep.isStepVisible().getObject())) {
                setActiveParentStepIndex(i);
                List<WizardStep> childrenSteps = getChildrenSteps(parentStep);
                int activeStepIndex = -1;
                for (int chi = 0; chi < childrenSteps.size(); chi++) {
                    WizardStep step = childrenSteps.get(chi);
                    if (BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                        activeStepIndex = chi;
                        break;
                    }
                }
                setActiveStepIndex(activeStepIndex);
                break;
            }
        }
    }

    protected void initNewStep(WizardStep newStep) {
        newStep.init(controller);
        if (newStep instanceof WizardParentStep parentStep) {
            getChildrenSteps(parentStep).forEach(s -> s.init(controller));
        }
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

    public void useLast() {
        setActiveParentStepById(getParentSteps().get(getParentSteps().size() - 1).getStepId());
    }
}
