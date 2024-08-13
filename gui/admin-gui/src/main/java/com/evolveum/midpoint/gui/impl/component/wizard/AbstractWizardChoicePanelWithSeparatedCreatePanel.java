/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class AbstractWizardChoicePanelWithSeparatedCreatePanel<C extends Containerable> extends AbstractWizardWithChoicePanel<C, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractWizardChoicePanelWithSeparatedCreatePanel.class);

    public AbstractWizardChoicePanelWithSeparatedCreatePanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createNewTypeWizard()));
    }

    private AbstractWizardPanel<C, ResourceDetailsModel> createNewTypeWizard() {
        WizardPanelHelper<C, ResourceDetailsModel> helper =
                new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                AbstractWizardChoicePanelWithSeparatedCreatePanel.this.onExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<C>> getDefaultValueModel() {
                return AbstractWizardChoicePanelWithSeparatedCreatePanel.this.getValueModel();
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = AbstractWizardChoicePanelWithSeparatedCreatePanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    getHelper().refreshValueModel();
                    showTypePreviewFragment(target);
                }
                return result;
            }
        };
        return createNewTypeWizard(getIdOfChoicePanel(), helper);
    }

    protected abstract AbstractWizardPanel<C, ResourceDetailsModel> createNewTypeWizard(
            String id, WizardPanelHelper<C, ResourceDetailsModel> helper);

    protected void showTypePreviewFragment(AjaxRequestTarget target) {
        showChoiceFragment(target, createTypePreview());
    }
}
