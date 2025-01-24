/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

public class FormSessionOptionPanel<ODM extends ObjectDetailsModels<?>>
        extends BasePanel<ODM> {

    private static final String ID_FORM = "form";

    public FormSessionOptionPanel(String id, IModel<ODM> model) {
        super(id, model);
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
        IModel<? extends PrismContainerWrapper<?>> containerFormModel = getContainerFormModel();
        VerticalFormPanel panel = new VerticalFormPanel(ID_FORM, containerFormModel, settings, getContainerConfiguration()) {

            @Override
            protected IModel<String> getTitleModel() {
                return getFormTitle();
            }

            @Override
            protected boolean isVisibleSubContainer(PrismContainerWrapper c) {
                return true;
            }

        };
        panel.setOutputMarkupId(true);
        add(panel);
    }


    protected boolean isExpanded() {
        return false;
    }

    protected boolean isVisibleSubContainer(PrismContainerWrapper c) {
        return false;
    }

    public ContainerPanelConfigurationType getContainerConfiguration() {
        return null;
    }

    protected IModel<? extends PrismContainerWrapper<?>> getContainerFormModel() {
        return this.getModelObject().getObjectWrapperModel();
    }

    protected ItemVisibilityHandler getVisibilityHandler() {
        return null;
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        return itemWrapper.isMandatory();
    }

    protected VerticalFormPanel getVerticalForm() {
        return (VerticalFormPanel) get(ID_FORM);
    }

    protected IModel<String> getFormTitle() {
        return Model.of();
    }
}
