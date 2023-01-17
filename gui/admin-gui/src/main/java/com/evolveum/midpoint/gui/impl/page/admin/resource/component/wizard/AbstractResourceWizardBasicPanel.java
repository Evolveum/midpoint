/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class AbstractResourceWizardBasicPanel<C extends Containerable> extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final String DEFAULT_SAVE_KEY = "WizardPanel.submit";
    private final WizardPanelHelper<C, ResourceDetailsModel> superHelper;

    public AbstractResourceWizardBasicPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> superHelper) {
        super(id, superHelper.getDetailsModel());
        this.superHelper = superHelper;
    }

    protected void onSaveResourcePerformed(AjaxRequestTarget target) {
        if (!isSavedAfterWizard()) {
            onExitPerformedAfterValidate(target);
            return;
        }
        OperationResult result = superHelper.onSaveObjectPerformed(target);
        if (result != null && !result.isError()) {
            WebComponentUtil.createToastForUpdateObject(target, ResourceType.COMPLEX_TYPE);
            onExitPerformedAfterValidate(target);
        } else {
            target.add(getFeedback());
        }
    }

    @Override
    protected void onExitPerformed(AjaxRequestTarget target) {
        if (isValid(target)) {
            onExitPerformedAfterValidate(target);
        }
    }

    protected boolean isValid(AjaxRequestTarget target) {
        return true;
    }

    protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
        super.onExitPerformed(target);
        superHelper.onExitPerformed(target);
    }

    private boolean isSavedAfterWizard() {
        return superHelper.isSavedAfterWizard();
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        if (isSavedAfterWizard()) {
            return getPageBase().createStringResource(getSaveLabelKey());
        }
        return getPageBase().createStringResource("WizardPanel.confirm");
    }

    @Override
    protected String getSubmitIcon() {
        if (isSavedAfterWizard()) {
            return super.getSubmitIcon();
        }
        return "fa fa-check";
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (isValid(target)) {
            onSaveResourcePerformed(target);
        }
    }

    protected String getSaveLabelKey() {
        return DEFAULT_SAVE_KEY;
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<C>> getValueModel() {
        return superHelper.getValueModel();
    }
}
