/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.AnalysisAttributeSelectorPanel;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class VerticalFormRoleAnalysisAttributeSettingPanel extends VerticalFormPrismPropertyPanel<ItemPathType>  {

    private static final String ID_ATTRIBUTE = "attribute";

    public VerticalFormRoleAnalysisAttributeSettingPanel(String id, IModel<PrismPropertyWrapper<ItemPathType>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createValuesPanel() {
        AnalysisAttributeSelectorPanel attributeSelectorPanel = new AnalysisAttributeSelectorPanel(ID_ATTRIBUTE, getModel());
        attributeSelectorPanel.setOutputMarkupId(true);
        return attributeSelectorPanel;
    }
}
