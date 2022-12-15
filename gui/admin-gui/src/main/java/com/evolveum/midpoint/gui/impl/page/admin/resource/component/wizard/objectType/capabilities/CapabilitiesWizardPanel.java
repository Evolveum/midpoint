/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.capabilities;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanelHelper;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */

@Experimental
public class CapabilitiesWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    public CapabilitiesWizardPanel(String id, ResourceWizardPanelHelper<ResourceObjectTypeDefinitionType> helper) {
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
