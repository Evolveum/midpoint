/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.withnavigation;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Page;
import org.apache.wicket.util.io.IClusterable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWizardController<AH extends AssignmentHolderType, ADM extends AssignmentHolderDetailsModel<AH>> extends WizardModelWithParentSteps implements IClusterable {

    private final WizardPanelHelper<? extends Containerable, ADM> helper;

    private List<AbstractWizardPartItem<AH, ADM>> partItems;

    private int indexOfActivePart;

    protected AbstractWizardController(WizardPanelHelper<? extends Containerable, ADM> helper) {
        this.helper = helper;
    }

    @Override
    public void init(Page page) {
        partItems = createWizardPartItems();

        partItems.forEach(partItem -> partItem.init(getHelper().getDetailsModel().getPageAssignmentHolder(), this));

        for (int i = 0; i < partItems.size(); i++) {
            AbstractWizardPartItem<AH, ADM> partItem = partItems.get(i);
            if (!partItem.isComplete()) {
                indexOfActivePart = i;
                break;
            }
            if (i == partItems.size() - 1) {
                partItem.useLast();
                indexOfActivePart = i;
            }
        }

        super.init(page);
    }

    protected final AssignmentHolderDetailsModel<AH> getObjectDetailsModel() {
        return helper.getDetailsModel();
    }

    public final WizardPanelHelper<? extends Containerable, ADM> getHelper() {
        return helper;
    }

    @NotNull
    protected abstract List<AbstractWizardPartItem<AH, ADM>> createWizardPartItems();

    private AbstractWizardPartItem<AH, ADM> getActivePartItem() {
        return partItems.get(indexOfActivePart);
    }

    public WizardStep getActiveStep() {
        return getActivePartItem().getActiveStep();
    }

    public void setActiveStepById(String id) {
        getActivePartItem().setActiveStepById(id);
    }

    public int getActiveStepIndex() {
        return getActivePartItem().getActiveStepIndex();
    }

    public int getActiveParentStepIndex() {
        int index = 0;
        for (int i = 0; i < partItems.size(); i++) {
            if (i == indexOfActivePart) {
                break;
            }
            AbstractWizardPartItem<AH, ADM> partItem = partItems.get(i);
            index += partItem.getParentSteps().stream()
                    .filter(parentStep -> BooleanUtils.isTrue(parentStep.isStepVisible().getObject()))
                    .toList()
                    .size();
        }
        index += getActivePartItem().getActiveParentStepIndex();
        return index;
    }

    public boolean hasNext() {
        return true;
    }

    public void next() {
        if (!getActivePartItem().hasNext()) {
            resolveFinishPart(getActivePartItem());
            String oldPanelId = getActiveStep().getStepId();
            removeOperationResult(oldPanelId);
            fireActiveStepChanged(getActiveStep());
        } else {
            getActivePartItem().next();
        }
    }

    protected abstract void resolveFinishPart(AbstractWizardPartItem<AH, ADM> activeStatus);

    public boolean hasPrevious() {
        return getActivePartItem().hasPrevious();
    }

    public void previous() {
        getActivePartItem().previous();
    }

    public WizardStep getNextPanel() {
        return getActivePartItem().getNextPanel();
    }

    public List<WizardStep> getActiveChildrenSteps() {
        return getActivePartItem().getActiveChildrenSteps();
    }

    public WizardParentStep getActiveParentStep() {
        return getActivePartItem().getActiveParentStep();
    }

    public void setActiveParentStepById(String id) {
        getActivePartItem().setActiveParentStepById(id);
    }

    public List<WizardParentStep> getParentSteps() {
        return getActivePartItem().getParentSteps();
    }

    public void addStepAfter(WizardStep newStep, Class<?> objectClassConnectorStepPanelClass) {
        getActivePartItem().addStepAfter(newStep, objectClassConnectorStepPanelClass);
    }

//    protected final void addWizardPartOnEnd(AbstractWizardPartItem<AH, ADM> newConnectorDevPartItem) {
//        statusItems.add(newConnectorDevPartItem);
//        setActiveWizardPartIndex(statusItems.size() - 1);
//    }

    protected void setActiveWizardPartIndex(int index) {
        this.indexOfActivePart = index;
        getActivePartItem().init(getHelper().getDetailsModel().getPageAssignmentHolder(), this);
    }

    protected final List<AbstractWizardPartItem<AH, ADM>> getPartItems() {
        return partItems;
    }

    protected final void showSummaryPanel() {
        setActiveWizardPartIndex(partItems.size() - 1);
    }

    protected final int addWizardPartAfter(AbstractWizardPartItem<AH, ADM> newPartItem, Enum<?> identifier) {
        List<AbstractWizardPartItem<AH, ADM>> newPartItems = new ArrayList<>();
        boolean added = false;
        for (AbstractWizardPartItem<AH, ADM> partItem : partItems) {
            if (!added && partItem.getIdentifierForWizardStatus() == identifier) {
                newPartItems.add(partItem);
                newPartItems.add(newPartItem);
                added = true;
            } else {
                newPartItems.add(partItem);
            }
        }
        if (!added) {
            newPartItems.add(newPartItem);
        }
        partItems = newPartItems;
        return partItems.indexOf(newPartItem);
    }

    public List<WizardParentStep> getAllParentSteps() {
        List<WizardParentStep> list = new ArrayList<>();
        partItems.stream()
                .map(
                        partItem -> partItem.getParentSteps().stream()
                                .filter(parentStep -> BooleanUtils.isTrue(parentStep.isStepVisible().getObject()))
                                .toList())
                .forEach(list::addAll);
        return list;
    }

    protected final PrismContainerWrapper<AH> getObjectWrapper() {
        return helper.getDetailsModel().getObjectWrapper();
    }

    @Override
    public List<? extends WizardStep> getSteps() {
        return getParentSteps();
    }

    @Override
    public WizardStep findPreviousStep() {
        return getActivePartItem().findPreviousStep(false);
    }
}
