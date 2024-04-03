/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismReferenceValuePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class AbstractFormWizardStepPanel<ODM extends ObjectDetailsModels>
        extends AbstractWizardStepPanel<ODM> {

    private static final String ID_FORM = "form";

    public AbstractFormWizardStepPanel(ODM model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler())
                .mandatoryHandler(this::checkMandatory)
                .build();
        VerticalFormPanel panel = new VerticalFormPanel(ID_FORM, getContainerFormModel(), settings, getContainerConfiguration()) {
            @Override
            protected String getIcon() {
                return AbstractFormWizardStepPanel.this.getIcon();
            }

            @Override
            protected IModel<?> getTitleModel() {
                return getFormTitle();
            }

            @Override
            protected WrapperContext createWrapperContext() {
                return getDetailsModel().createWrapperContext();
            }

            @Override
            protected boolean isVisibleSubContainer(PrismContainerWrapper c) {
                return AbstractFormWizardStepPanel.this.isVisibleSubContainer(c);
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    protected boolean isVisibleSubContainer(PrismContainerWrapper c) {
        return false;
    }

    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return this.getDetailsModel().getObjectWrapperModel();
    }

    protected ItemVisibilityHandler getVisibilityHandler() {
        return null;
    }

    protected abstract String getIcon();

    protected ContainerPanelConfigurationType getContainerConfiguration() {
        return getContainerConfiguration(getPanelType());
    }

    @Override
    public String getStepId() {
        return getPanelType();
    }

    protected abstract String getPanelType();

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        return itemWrapper.isMandatory();
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        getVerticalForm().visitChildren(
                VerticalFormPrismPropertyValuePanel.class,
                (component, objectIVisit) -> ((VerticalFormPrismPropertyValuePanel<?>) component).updateFeedbackPanel(target));

        getVerticalForm().visitChildren(
                VerticalFormPrismReferenceValuePanel.class,
                (component, objectIVisit) -> ((VerticalFormPrismReferenceValuePanel<?>) component).updateFeedbackPanel(target));
    }

    protected VerticalFormPanel getVerticalForm() {
        return (VerticalFormPanel) get(ID_FORM);
    }
}
