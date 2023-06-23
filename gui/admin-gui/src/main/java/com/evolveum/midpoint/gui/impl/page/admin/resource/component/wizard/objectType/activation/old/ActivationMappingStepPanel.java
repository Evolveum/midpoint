/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation.old;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */

@Experimental
public abstract class ActivationMappingStepPanel extends AbstractWizardStepPanel {

    protected static final String ID_PANEL = "panel";

    private final IModel<PrismContainerWrapper<ResourceBidirectionalMappingType>> containerModel;
    private final ResourceDetailsModel resourceModel;

    public ActivationMappingStepPanel(ResourceDetailsModel model,
                             IModel<PrismContainerWrapper<ResourceBidirectionalMappingType>> containerModel) {
        super(model);
        this.containerModel = containerModel;
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        SingleContainerPanel panel;
        if (getContainerConfiguration() == null) {
            panel = new SingleContainerPanel(ID_PANEL, getContainerFormModel(), ResourcePasswordDefinitionType.COMPLEX_TYPE);
        } else {
            panel = new SingleContainerPanel(ID_PANEL, getContainerFormModel(), getContainerConfiguration());
        }
        panel.setOutputMarkupId(true);
        panel.add(AttributeAppender.append("class", "card col-12"));
        add(panel);
    }

    protected ContainerPanelConfigurationType getContainerConfiguration() {
        return WebComponentUtil.getContainerConfiguration(resourceModel.getObjectDetailsPageConfiguration().getObject(), getPanelType());
    }

    protected abstract String getPanelType();

    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return containerModel;
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        target.add(getPageBase().getFeedbackPanel());
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }
}
