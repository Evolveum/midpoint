/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

/**
 * @author lskublik
 */
public abstract class AbstractVerifiableWizardBasicPanel<C extends Containerable, AHD extends AssignmentHolderDetailsModel> extends AbstractWizardBasicPanel<AHD> {

    private static final String DEFAULT_SAVE_KEY = "WizardPanel.submit";
    private final WizardPanelHelper<C, AHD> superHelper;

    public AbstractVerifiableWizardBasicPanel(
            String id,
            WizardPanelHelper<C, AHD> superHelper) {
        super(id, superHelper.getDetailsModel());
        this.superHelper = superHelper;
    }

    protected void onSaveResourcePerformed(AjaxRequestTarget target) {
        OperationResult result = superHelper.onSaveObjectPerformed(target);
        if (result != null && !result.isError()) {
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
        superHelper.onExitPerformedAfterValidate(target);
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource(getSaveLabelKey());
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

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-8";
    }

    protected ContainerPanelConfigurationType getConfiguration(){
        if (StringUtils.isEmpty(getPanelType())) {
            return null;
        }
        return WebComponentUtil.getContainerConfiguration(
                (GuiObjectDetailsPageType) getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                getPanelType());
    }

    protected String getPanelType() {
        return null;
    }

    public WizardPanelHelper<C, AHD> getHelper() {
        return superHelper;
    }
}
