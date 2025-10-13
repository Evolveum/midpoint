/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.capabilities;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.CapabilitiesPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
@Experimental
public abstract class CapabilitiesWizardStepPanel extends AbstractResourceWizardBasicPanel<CapabilityCollectionType> {

    private static final String ID_PANEL = "panel";

    public CapabilitiesWizardStepPanel(
            String id,
            WizardPanelHelper<CapabilityCollectionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CapabilitiesPanel panel = new CapabilitiesPanel(ID_PANEL, getAssignmentHolderDetailsModel(), getValueModel());
        panel.setOutputMarkupId(true);
        add(panel);
    }

    @Override
    protected String getSaveLabelKey() {
        return "CapabilitiesWizardStepPanel.saveButton";
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CapabilitiesWizardStepPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CapabilitiesWizardStepPanel.subText");
    }
}
