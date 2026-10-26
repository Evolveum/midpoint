/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.withnavigation;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.io.IClusterable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWizardController<AH extends AssignmentHolderType, ADM extends AssignmentHolderDetailsModel<AH>> extends WizardModelWithParentSteps implements IClusterable {

    private final WizardPanelHelper<? extends Containerable, ADM> helper;

    private List<AbstractWizardPartItem<AH, ADM>> partItems;

    private int indexOfActivePart = -1;
    private int indexOfInProgressPart = -1;

    private WizardStep summaryPanel;
    private boolean showSummaryPanel = false;

    protected AbstractWizardController(WizardPanelHelper<? extends Containerable, ADM> helper) {
        this.helper = helper;
    }

    @Override
    public void init(Page page) {
        partItems = createWizardPartItems();
        refresh();
    }

    protected final void refresh() {
        indexOfActivePart = -1;
        indexOfInProgressPart = -1;

        partItems.forEach(partItem -> partItem.init(getHelper().getDetailsModel().getPageAssignmentHolder(), this));

        showSummaryPanel = true;
        for (int i = 0; i < partItems.size(); i++) {
            AbstractWizardPartItem<AH, ADM> partItem = partItems.get(i);
            if (!partItem.isComplete()) {
                indexOfInProgressPart = i;
                indexOfActivePart = i;
                showSummaryPanel = false;
                break;
            }
        }

        fireActiveStepChanged(getActiveStep());
    }

    protected final void setPartItems(List<AbstractWizardPartItem<AH, ADM>> partItems) {
        this.partItems = partItems;
    }

    @Override
    public void invalidateChildrenSteps(String parentStepId) {
        partItems.forEach(partItem -> partItem.invalidateChildrenSteps(parentStepId));
    }

    protected final ADM getObjectDetailsModel() {
        return helper.getDetailsModel();
    }

    public final WizardPanelHelper<? extends Containerable, ADM> getHelper() {
        return helper;
    }

    @NotNull
    protected abstract List<AbstractWizardPartItem<AH, ADM>> createWizardPartItems();

    private AbstractWizardPartItem<AH, ADM> getActivePartItem() {
        if (indexOfActivePart == -1) {
            return null;
        }
        return partItems.get(indexOfActivePart);
    }

    private AbstractWizardPartItem<AH, ADM> getInProgressPartItem() {
        if (indexOfInProgressPart == -1) {
            return null;
        }
        return partItems.get(indexOfInProgressPart);
    }

    private WizardStep getSummaryPanel() {
        if (summaryPanel == null) {
            summaryPanel = createSummaryPanel();
            summaryPanel.init(this);
        }
        return summaryPanel;
    }

    protected abstract WizardStep createSummaryPanel();

    public WizardStep getActiveStep() {
        if (showSummaryPanel) {
            return getSummaryPanel();
        }

        return getActivePartItem().getActiveStep();
    }

    public void setActiveStepById(String id) {
        for (int i = 0; i < partItems.size(); i++) {
            AbstractWizardPartItem<AH, ADM> partItem = partItems.get(i);
            if (partItem.setActiveStepById(id)) {
                int exactlyActiveParentStepIndex = partItem.getActiveParentStepIndex();
                int exactlyActiveStepIndex = partItem.getActiveStepIndex();
                setActiveWizardPartIndex(i);
                if (exactlyActiveParentStepIndex != partItem.getActiveParentStepIndex()) {
                    partItem.setActiveParentStepIndex(exactlyActiveParentStepIndex);
                    partItem.setActiveStepIndex(exactlyActiveStepIndex);
                } else if (exactlyActiveStepIndex == -1 || partItem.getActiveStepIndex() == -1){
                    partItem.setActiveStepIndex(exactlyActiveStepIndex);
                }
                return;
            }
        }
    }

    @Override
    public void setActiveStepWithinActivePart(String id) {
        AbstractWizardPartItem<AH, ADM> activePartItem = getActivePartItem();
        if (activePartItem != null) {
            activePartItem.setActiveStepById(id);
        }
    }

    public int getActiveStepIndex() {
        if (indexOfActivePart == -1) {
            return -1;
        }
        return getActivePartItem().getActiveStepIndex();
    }

    public int getActiveParentStepIndex() {
        AbstractWizardPartItem<AH, ADM> activePartItem = getActivePartItem();
        return getParentStepIndex(indexOfActivePart, activePartItem == null ? 0 : activePartItem.getActiveParentStepIndex());
    }

    public int getInProgressStepIndex() {
        if (indexOfInProgressPart == -1) {
            return -1;
        }
        return getInProgressPartItem().getInProgressStepIndex();
    }

    public int getInProgressParentStepIndex() {
        AbstractWizardPartItem<AH, ADM> inProgressPartItem = getInProgressPartItem();
        return getParentStepIndex(indexOfInProgressPart, inProgressPartItem == null ? 0 : inProgressPartItem.getInProgressParentStepIndex());
    }

    private int getParentStepIndex(int indexOfPart, int indexOfParenStep) {
        int index = 0;
        for (int i = 0; i < partItems.size(); i++) {
            if (i == indexOfPart) {
                break;
            }
            AbstractWizardPartItem<AH, ADM> partItem = partItems.get(i);
            index += partItem.getParentSteps().stream()
                    .filter(parentStep -> BooleanUtils.isTrue(parentStep.isStepVisible().getObject()))
                    .toList()
                    .size();
        }
        if (indexOfPart != -1) {
            index += indexOfParenStep;
        }
        return index;
    }

    public boolean hasNext() {
        return true;
    }

    public void next() {
        if (showSummaryPanel) {
            return;
        }

        if (!getActivePartItem().hasNext()) {
            resolveFinishPart(getActivePartItem());
            String oldPanelId = getActiveStep().getStepId();
            removeOperationResult(oldPanelId);
            fireActiveStepChanged(getActiveStep());
        } else {
            getActivePartItem().next();
        }
    }

    protected void resolveFinishPart(AbstractWizardPartItem<AH, ADM> activeStatus) {
        if (indexOfActivePart == indexOfInProgressPart) {
            clearInProgressPart();
        }
        indexOfActivePart = -1;
        showSummaryPanel();
    }

    protected void clearInProgressPart() {
        indexOfInProgressPart = -1;
    }

    protected void clearActivePart() {
        indexOfActivePart = -1;
    }

    public boolean hasPrevious() {
        // The summary panel is always one level above any wizard part, so back navigation
        // exists as long as we are not already on the summary.
        return !showSummaryPanel && getActivePartItem() != null;
    }

    public void previous() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class).orElse(null);
        if (target != null && hasUnsavedChanges()) {
            showUnsavedStepChangesConfirmation(target, t -> doPrevious());
            return;
        }
        doPrevious();
    }

    private void doPrevious() {
        if (showSummaryPanel) {
            return;
        }

        AbstractWizardPartItem<AH, ADM> activePartItem = getActivePartItem();
        if (activePartItem == null || !activePartItem.hasPrevious()) {
            showSummaryPanel();
            fireActiveStepChanged(getActiveStep());
        } else {
            activePartItem.previous();
        }
    }

    /**
     * Whether there are unsaved changes that would be lost by navigating away from the active step:
     * in the main object wrapper, in the separate testing resource wrapper (for steps editing it -
     * checked generically, so it keeps working for every step that happens to use the testing
     * resource, not just the ones we know about today), or outside any Prism wrapper entirely (e.g.
     * a live script editor - {@link WizardStep#hasUnsavedStepChanges()}, which individual steps with that kind
     * of state override themselves, since it can't be detected generically).
     */
    boolean hasUnsavedChanges() {
        boolean mainWrapperDelta = getHelper().getDetailsModel().hasDelta();
        boolean testingResourceDelta = getHelper().getDetailsModel() instanceof ConnectorDevelopmentDetailsModel connectorDetailsModel
                && ConnectorDevelopmentWizardUtil.hasUnsavedTestingResourceChanges(connectorDetailsModel);
        boolean stepChanges = getActiveStep().hasUnsavedStepChanges();
        return mainWrapperDelta || testingResourceDelta || stepChanges;
    }

    void showUnsavedStepChangesConfirmation(AjaxRequestTarget target, SerializableConsumer<AjaxRequestTarget> afterAction) {
        PageBase page = WebComponentUtil.getPageBase(getPanel());
        UnsavedStepChangesConfirmationPanel popup = new UnsavedStepChangesConfirmationPanel(page.getMainPopupBodyId()) {
            @Override
            protected void onSavePerformed(AjaxRequestTarget target) {
                if (getActiveStep().hasUnsavedStepChanges()) {
                    if (!getActiveStep().saveUnsavedStepChanges(target)) {
                        return;
                    }
                } else {
                    getHelper().onSaveObjectPerformed(target);
                }
                afterAction.accept(target);
                target.add(getPanel());
            }

            @Override
            protected void onDiscardPerformed(AjaxRequestTarget target) {
                page.hideMainPopup(target);
                getHelper().getDetailsModel().reloadPrismObjectModel();
                getHelper().refreshValueModel();
                if (getHelper().getDetailsModel() instanceof ConnectorDevelopmentDetailsModel connectorDetailsModel) {
                    ConnectorDevelopmentWizardUtil.discardTestingResourceChanges(connectorDetailsModel);
                }
                getActiveStep().discardUnsavedStepChanges();
                afterAction.accept(target);
                target.add(getPanel());
            }
        };
        page.showMainPopup(popup, target);
    }

    public WizardStep getNextPanel() {
        if (showSummaryPanel) {
            return null;
        }

        return getActivePartItem().getNextPanel();
    }

    public List<WizardStep> getActiveChildrenSteps() {
        if (indexOfActivePart == -1) {
            return List.of();
        }

        return getActivePartItem().getActiveChildrenSteps();
    }

    public List<WizardStep> getInProgressChildrenSteps() {
        if (indexOfInProgressPart == -1) {
            return getActiveChildrenSteps();
        }

        return getInProgressPartItem().getInProgressChildrenSteps();
    }

    public WizardParentStep getActiveParentStep() {
        if (indexOfActivePart == -1) {
            return null;
        }

        return getActivePartItem().getActiveParentStep();
    }

    public WizardParentStep getInProgressParentStep() {
        if (indexOfInProgressPart == -1) {
            return getActiveParentStep();
        }

        return getInProgressPartItem().getInProgressParentStep();
    }

    public List<WizardParentStep> getParentSteps() {
        if (indexOfActivePart == -1) {
            return List.of();
        }

        return getActivePartItem().getParentSteps();
    }

    protected void setActiveWizardPartIndex(int index) {
        showSummaryPanel = false;
        this.indexOfActivePart = index;
        if (indexOfActivePart != indexOfInProgressPart || !getActivePartItem().isInitialized()) {
            getActivePartItem().init(getHelper().getDetailsModel().getPageAssignmentHolder(), this);
        }
        if (!getActivePartItem().isComplete() && indexOfInProgressPart < indexOfActivePart) {
            indexOfInProgressPart = indexOfActivePart;
        }
    }

    protected final List<AbstractWizardPartItem<AH, ADM>> getPartItems() {
        return partItems;
    }

    public final void showSummaryPanel() {
        showSummaryPanel = true;
    }

    public void useInProgressStepOrShowSummary() {
        if (indexOfInProgressPart != -1) {
            setActiveWizardPartIndex(indexOfInProgressPart);
            getActivePartItem().useInProgressStepAsActive();
        } else {
            showSummaryPanel();
        }
    }

    @Override
    public void clearChildStep() {
        getActivePartItem().clearActiveStep();
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

    protected final int addWizardPartOnEnd(AbstractWizardPartItem<AH, ADM> newPartItem) {
        partItems.add(newPartItem);
        return partItems.indexOf(newPartItem);
    }

    public List<WizardParentStep> getAllParentSteps() {
        List<WizardParentStep> list = new ArrayList<>();
        partItems.stream()
                .map(AbstractWizardPartItem::getParentSteps)
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
        if (showSummaryPanel) {
            return null;
        }

        AbstractWizardPartItem<AH, ADM> activePartItem = getActivePartItem();
        if (activePartItem == null) {
            return null;
        }

        WizardStep previous = activePartItem.findPreviousStep(false);
        return previous != null ? previous : getSummaryPanel();
    }

    @Override
    public boolean isShowedSummary() {
        return showSummaryPanel;
    }

    protected final void removeActivePartItem() {
        partItems.remove(getActivePartItem());
    }

    public boolean isActiveAndInProgressParentSame() {
        if (indexOfActivePart == -1 || indexOfInProgressPart == -1) {
            return false;
        }
        return indexOfInProgressPart == indexOfActivePart && getInProgressParentStepIndex() == getActiveParentStepIndex();
    }
}
