/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.AnalysisAttributeSelectorPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisAttributeSettingType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class VerticalFormAnalysisAttributesPanel extends VerticalFormPrismContainerPanel<AnalysisAttributeSettingType> {

    private static final String ID_ATTRIBUTES = "attributes";

    public VerticalFormAnalysisAttributesPanel(String id, IModel<PrismContainerWrapper<AnalysisAttributeSettingType>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createValuesPanel() {
        PrismPropertyWrapper<ItemPathType> property;
        property = getItemPathTypePrismPropertyWrapper();
        PrismContainerWrapper<AnalysisAttributeSettingType> object = getModel().getObject();

        AnalysisAttributeSelectorPanel attributeSelectorPanel = new AnalysisAttributeSelectorPanel(ID_ATTRIBUTES, Model.of(property)){
            @Override
            public boolean isEditable() {
                if(getSettings() == null || getSettings().getEditabilityHandler() == null){
                    return true;
                }
                return getSettings().getEditabilityHandler().isEditable(getModelObject());
            }
        };
        attributeSelectorPanel.setOutputMarkupId(true);
        return attributeSelectorPanel;

    }

    private PrismPropertyWrapper<ItemPathType> getItemPathTypePrismPropertyWrapper() {
        PrismPropertyWrapper<ItemPathType> property;
        try {
            property = getModelObject().findProperty(AnalysisAttributeSettingType.F_PATH);
        } catch (SchemaException e) {
            throw new SystemException("Cannot find property", e);
        }
        return property;
    }

    @Override
    protected boolean isHelpTextVisible() {
        return true;
    }

    @Override
    protected boolean isExpandedButtonVisible() {
        return false;
    }

    @Override
    protected String getCssForHeader() {
        return "pb-0 pt-2 pl-3 pr-3 text-gray font-weight-bold text-center w-100";
    }
}
