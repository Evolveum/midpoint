/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.capabilities;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

/**
 * @author lskublik
 */

@Experimental
public class CapabilitiesWizardPanel extends AbstractWizardPanel<CapabilityCollectionType, ResourceDetailsModel> {

    public CapabilitiesWizardPanel(String id, WizardPanelHelper<CapabilityCollectionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createCapabilityPanel()));
    }

    private CapabilitiesWizardStepPanel createCapabilityPanel() {
        CapabilitiesWizardStepPanel panel = new CapabilitiesWizardStepPanel(
                getIdOfChoicePanel(),
                getHelper()) {
        };
        panel.setOutputMarkupId(true);
        return panel;
    }
}
