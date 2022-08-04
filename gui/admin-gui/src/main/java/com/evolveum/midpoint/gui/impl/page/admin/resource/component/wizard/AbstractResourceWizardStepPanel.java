/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class AbstractResourceWizardStepPanel extends BasicWizardStepPanel {

    private final ResourceDetailsModel resourceModel;
    public AbstractResourceWizardStepPanel(ResourceDetailsModel model){
        this.resourceModel = model;
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        if (getWizard().getNextPanel() == null) {
            return getPageBase().createStringResource("SelectObjectClassesStepPanel.nextLabel");
        }
        return super.getNextLabelModel();
    }

    protected void onFinishPerformed(AjaxRequestTarget target) {
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-8";
    }
}
