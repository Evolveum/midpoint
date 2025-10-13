/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingTile;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;

public class CreateActivationMappingTile extends TemplateTile<PrismContainerDefinition> {

    private boolean canCreateNewValue = true;

    private final MappingTile.MappingDefinitionType mappingDefinitionType;
    public CreateActivationMappingTile(PrismContainerDefinition definition) {
        super(definition);
        if (QNameUtil.match(definition.getTypeName(), ResourceBidirectionalMappingType.COMPLEX_TYPE)) {
            this.mappingDefinitionType = MappingTile.MappingDefinitionType.CONFIGURED;
        } else {
            this.mappingDefinitionType = MappingTile.MappingDefinitionType.PREDEFINED;
        }
    }

    public void setCanCreateNewValue(boolean canCreateNewValue) {
        this.canCreateNewValue = canCreateNewValue;
    }

    public boolean canCreateNewValue() {
        return canCreateNewValue;
    }

    public MappingTile.MappingDefinitionType getMappingDefinitionType() {
        return mappingDefinitionType;
    }
}
