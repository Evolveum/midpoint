/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author lskublik
 */
public abstract class AbstractWizardWithChoicePanel<C extends Containerable, AHD extends AssignmentHolderDetailsModel> extends AbstractWizardPanel<C, AHD> {

    private boolean showChoicePanel = false;

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

    protected  <V extends Containerable> WizardPanelHelper<V, AHD> createHelper(ItemPath path, boolean isWizardFlow) {
        return new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target);
            }

            @Override
            public void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                getAssignmentHolderModel().reloadPrismObjectModel();
                getHelper().refreshValueModel();
                showTypePreviewFragment(target);
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
                getAssignmentHolderModel().reloadPrismObjectModel();
                getHelper().refreshValueModel();
                showTypePreviewFragment(target);
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

    protected void checkDeltasExitPerformed(AjaxRequestTarget target) {

        if (!((PageAssignmentHolderDetails)getPageBase()).hasUnsavedChanges(target)) {
            getAssignmentHolderModel().reloadPrismObjectModel();
            getHelper().refreshValueModel();
            showTypePreviewFragment(target);
            return;
        }
        ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("OperationalButtonsPanel.confirmBack")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                getAssignmentHolderModel().reloadPrismObjectModel();
                getHelper().refreshValueModel();
                showTypePreviewFragment(target);
            }
        };

        getPageBase().showMainPopup(confirmationPanel, target);
    }

    protected abstract void showTypePreviewFragment(AjaxRequestTarget target);
}
