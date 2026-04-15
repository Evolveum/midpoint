/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

public class MappingTile<T extends PrismContainerValueWrapper<? extends Containerable>> extends TemplateTile<T> {

    public enum MappingDefinitionType {
        CONFIGURED,
        PREDEFINED
    }

    private final MappingDefinitionType mappingDefinitionType;

    private String help;
    public MappingTile(T mapping) {
        super(mapping);
        if (QNameUtil.match(mapping.getDefinition().getTypeName(), MappingType.COMPLEX_TYPE)) {
            this.mappingDefinitionType = MappingDefinitionType.CONFIGURED;
        } else {
            this.mappingDefinitionType = MappingDefinitionType.PREDEFINED;
        }
    }

    public MappingDefinitionType getMappingDefinitionType() {
        return mappingDefinitionType;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public String getHelp() {
        return help;
    }
}
