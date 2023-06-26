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
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;

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
