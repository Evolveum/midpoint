/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.objectRef;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;

public class ReferenceMappingWizardPanel extends AttributeMappingWizardPanel<ShadowAssociationDefinitionType> {

    public ReferenceMappingWizardPanel(String id, WizardPanelHelper<ShadowAssociationDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected ItemName getItemNameOfContainerWithMappings() {
        return ShadowAssociationDefinitionType.F_OBJECT_REF;
    }
}
