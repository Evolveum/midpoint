/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;

public class MappingTile extends TemplateTile<PrismContainerValueWrapper<? extends Containerable>> {

    enum MappingDefinitionType {
        CONFIGURED,
        PREDEFINED
    }

    private final MappingDefinitionType mappingDefinitionType;
    public MappingTile(PrismContainerValueWrapper<? extends Containerable> mapping) {
        super(mapping);
        if (QNameUtil.match(mapping.getDefinition().getTypeName(), ResourceBidirectionalMappingType.COMPLEX_TYPE)) {
            this.mappingDefinitionType = MappingDefinitionType.CONFIGURED;
        } else {
            this.mappingDefinitionType = MappingDefinitionType.PREDEFINED;
        }
    }
}
