/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyValuePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class AbstractResourceWizardStepPanel extends BasicWizardPanel {

    @Override
    protected IModel<String> getNextLabelModel() {
        if (getWizard().getNextPanel() == null) {
            return getPageBase().createStringResource("SelectObjectClassesStepPanel.nextLabel");
        }
        return super.getNextLabelModel();
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        if (getWizard().getNextPanel() == null) {
            onFinishWizardPerformed(target);
            return false;
        }
        return super.onNextPerformed(target);
    }

    protected void onFinishWizardPerformed(AjaxRequestTarget target) {
    }
}
