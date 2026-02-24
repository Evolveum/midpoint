/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

/**
 * @author lskublik
 */
public abstract class AbstractWizardWithChoicePanel<C extends Containerable, AHD extends AssignmentHolderDetailsModel> extends AbstractWizardPanel<C, AHD> {

    private boolean showChoicePanel = false;
    public boolean isFromTypePreview = false;

    public AbstractWizardWithChoicePanel(
            String id,
            WizardPanelHelper<C, AHD> helper) {
        super(id, helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (isShowChoicePanel()) {
            addOrReplace(createChoiceFragment(createTypePreview()));
        }
    }

    protected abstract Component createTypePreview();

    private boolean isShowChoicePanel() {
        return showChoicePanel;
    }

    public void setShowChoicePanel(boolean showChoicePanel) {
        this.showChoicePanel = showChoicePanel;
    }

    public void checkDeltasExitPerformed(AjaxRequestTarget target) {
        checkDeltasExitPerformed(target, null);
    }

    public void processDeltasExitPerform(AjaxRequestTarget target,
            @Nullable SerializableConsumer<AjaxRequestTarget> afterAction) {
        getAssignmentHolderModel().reloadPrismObjectModel();
        getHelper().refreshValueModel();
        if (afterAction != null) {
            afterAction.accept(target);
        } else {
            showAfterCheckDeltasExitPerformed(target);
        }
    }

    protected void showAfterCheckDeltasExitPerformed(AjaxRequestTarget target) {
        showTypePreviewFragment(target);
    }

    protected <V extends Containerable> WizardPanelHelper<V, AHD> createHelper(ItemPath path, boolean isWizardFlow) {
        return new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target);
            }

            @Override
            public void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<V>> getDefaultValueModel() {
                return PrismContainerValueWrapperModel.fromContainerValueWrapper(AbstractWizardWithChoicePanel.this.getValueModel(), path);
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = AbstractWizardWithChoicePanel.this.onSavePerformed(target);
                if (isWizardFlow && result != null && !result.isError()) {
                    getHelper().refreshValueModel();
                    showTypePreviewFragment(target);
                }
                return result;
            }
        };
    }

    protected WizardPanelHelper<C, AHD> createHelper(boolean isWizardFlow) {

        return new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target);
            }

            @Override
            public void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<C>> getDefaultValueModel() {
                return AbstractWizardWithChoicePanel.this.getValueModel();
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = AbstractWizardWithChoicePanel.this.onSavePerformed(target);
                if (isWizardFlow && result != null && !result.isError()) {
                    getHelper().refreshValueModel();
                    showTypePreviewFragment(target);
                }
                return result;
            }
        };
    }

    protected abstract void showTypePreviewFragment(AjaxRequestTarget target);
}
