/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.verticalForm.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.verticalForm.VerticalFormPrismPropertyValuePanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class AbstractResourceWizardStepPanel extends BasicWizardPanel {

    private static final String ID_FORM = "form";

    private final ResourceDetailsModel resourceModel;

    public AbstractResourceWizardStepPanel(ResourceDetailsModel model) {
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        iniLayout();
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-8";
    }

    private void iniLayout() {
        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(w -> checkVisibility(w))
                .mandatoryHandler(w -> checkMandatory(w))
                .build();
        VerticalFormPanel form = new VerticalFormPanel(ID_FORM, this.resourceModel.getObjectWrapperModel(), settings, getContainerConfiguration()) {
            @Override
            protected String getIcon() {
                return AbstractResourceWizardStepPanel.this.getIcon();
            }

            @Override
            protected IModel<?> getTitleModel() {
                return getTitle();
            }
        };
        form.add(AttributeAppender.append("class", "col-8"));
        add(form);
    }

    protected abstract String getIcon();

    private ContainerPanelConfigurationType getContainerConfiguration() {
        return WebComponentUtil.getContainerConfiguration(resourceModel.getObjectDetailsPageConfiguration().getObject(), getPanelType());
    }

    protected abstract String getPanelType();

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        return itemWrapper.isMandatory();
    }

    protected ItemVisibility checkVisibility(ItemWrapper itemWrapper) {
        return ItemVisibility.AUTO;
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        getVerticalForm().visitChildren(VerticalFormPrismPropertyValuePanel.class, (component, objectIVisit) -> {
            ((VerticalFormPrismPropertyValuePanel) component).updateFeedbackPanel(target);
        });
    }

    private MarkupContainer getVerticalForm() {
        return (MarkupContainer) get(ID_FORM);
    }
}
